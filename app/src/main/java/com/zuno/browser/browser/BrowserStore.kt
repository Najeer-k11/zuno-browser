package com.zuno.browser.browser

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zuno.browser.data.BrowserSettings
import com.zuno.browser.data.SettingsRepository
import com.zuno.browser.engine.GeckoEngine
import com.zuno.browser.media.MediaHub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebResponse
import java.io.File

/** A long-press on page content. */
data class ContextMenuRequest(val tab: Tab, val linkUri: String?, val srcUri: String?, val title: String?, val isImage: Boolean)

/**
 * The browser's state: workspaces, tabs and their Gecko sessions. Lives for the whole process so
 * sessions survive activity recreation. All methods are main-thread.
 */
class BrowserStore(private val context: Context, private val repo: SettingsRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val file = File(context.filesDir, "tabs.json")
    private val historyFile = File(context.filesDir, "history.json")

    val workspaces = mutableStateListOf<Workspace>()
    val tabs = mutableStateListOf<Tab>()
    val history = mutableStateListOf<HistoryEntry>()

    var activeWorkspaceId by mutableStateOf("")
        private set
    var selectedTabId by mutableStateOf<String?>(null)
        private set
    /** Last selected tab per workspace, so switching workspaces returns to where you were. */
    private val lastSelected = HashMap<String, String?>()

    var fullscreen by mutableStateOf(false)
        private set
    var prompt by mutableStateOf<PromptRequest?>(null)
    var contextMenu by mutableStateOf<ContextMenuRequest?>(null)

    /** Vertical scroll deltas of the selected tab (positive = content moved up / reading down). */
    var onScroll: ((Int) -> Unit)? = null
    /** Fired when a tab's session must be (re)attached to the GeckoView. */
    var sessionVersion by mutableStateOf(0)
        private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    var settings: BrowserSettings = BrowserSettings()
        private set

    private var saveJob: Job? = null
    private var pendingBlocked = 0L
    private var blockedJob: Job? = null

    val activeWorkspace: Workspace? get() = workspaces.firstOrNull { it.id == activeWorkspaceId }
    val selectedTab: Tab? get() = tabs.firstOrNull { it.id == selectedTabId }
    val essentials: List<Tab> get() = tabs.filter { it.essential && !it.private }

    fun tabsIn(workspaceId: String): List<Tab> = tabs.filter { it.workspaceId == workspaceId && !it.essential && !it.private }

    // -----------------------------------------------------------------------------------------
    // Private mode: an app-wide mode with its own tab set. Private tabs are never persisted,
    // never recorded in history, and run in Gecko's private browsing context.
    // -----------------------------------------------------------------------------------------

    var privateMode by mutableStateOf(false)
        private set
    private var lastPrivateSelected: String? = null
    private var lastNormalSelected: String? = null

    val privateTabs: List<Tab> get() = tabs.filter { it.private }

    /** Tabs of the current mode, in sidebar order (used for counts and swipe-to-switch). */
    val modeTabs: List<Tab> get() = if (privateMode) privateTabs else tabsIn(activeWorkspaceId)

    fun switchPrivateMode(on: Boolean) {
        if (on == privateMode) return
        val current = selectedTab
        if (on) lastNormalSelected = current?.id else lastPrivateSelected = current?.id
        privateMode = on
        val target = if (on) lastPrivateSelected?.let { id -> tabs.firstOrNull { it.id == id && it.private } } ?: privateTabs.lastOrNull()
        else lastNormalSelected?.let { id -> tabs.firstOrNull { it.id == id && !it.private } } ?: tabsIn(activeWorkspaceId).lastOrNull()
        selectTab(target)
    }

    /** Closes every private tab (Gecko drops the private session's cookies and storage with them). */
    fun closeAllPrivate() {
        privateTabs.forEach { t -> t.session?.let { MediaHub.forget(it); it.close() }; t.session = null; tabs.remove(t) }
        lastPrivateSelected = null
        if (privateMode) switchPrivateMode(false) else sessionVersion++
    }

    init {
        load()
        // Restore the selected tab's page right away; others wake lazily when selected.
        selectedTab?.takeIf { !it.isBlank }?.let { ensureSession(it) }
        GeckoEngine.onShieldReady = { tabs.forEach { t -> t.session?.let { attachShield(t, it) } } }
    }

    fun updateSettings(s: BrowserSettings) {
        val allowChanged = s.allowlist != settings.allowlist
        settings = s
        if (allowChanged) tabs.forEach { t -> t.session?.settings?.useTrackingProtection = !isAllowlisted(t.url) }
    }

    fun isAllowlisted(url: String): Boolean {
        val h = hostOf(url)
        if (h.isBlank()) return false
        return settings.allowlist.any { h == it || h.endsWith(".$it") }
    }

    // -----------------------------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------------------------

    private fun load() {
        runCatching {
            if (file.exists()) {
                val root = JSONObject(file.readText())
                root.optJSONArray("workspaces")?.let { a -> for (i in 0 until a.length()) workspaces += Workspace.fromJson(a.getJSONObject(i)) }
                root.optJSONArray("tabs")?.let { a -> for (i in 0 until a.length()) tabs += Tab.fromJson(a.getJSONObject(i)) }
                activeWorkspaceId = root.optString("active")
                selectedTabId = root.optString("selected").takeIf { s -> s.isNotBlank() && tabs.any { it.id == s } }
                root.optJSONObject("last")?.let { o -> o.keys().forEach { k -> lastSelected[k] = o.optString(k).ifBlank { null } } }
            }
        }
        if (workspaces.isEmpty()) {
            workspaces += Workspace.new("Personal", "leaf")
            workspaces += Workspace.new("Work", "briefcase")
        }
        if (workspaces.none { it.id == activeWorkspaceId }) activeWorkspaceId = workspaces.first().id
        // Orphaned tabs (workspace deleted while app was killed) land in the active workspace.
        tabs.filter { t -> workspaces.none { it.id == t.workspaceId } }.forEach { it.workspaceId = activeWorkspaceId }
        // No default tabs. Earlier builds seeded four Essentials; drop them if they were never opened.
        val seeded = setOf("https://wikipedia.org", "https://github.com", "https://news.ycombinator.com", "https://www.youtube.com")
        tabs.removeAll { it.essential && it.url in seeded && it.savedState == null }
        // One kind of pinning now: pinned tabs live in the grid at the top of the sidebar.
        tabs.filter { it.pinned }.forEach { it.pinned = false; it.essential = true }
        runCatching {
            if (historyFile.exists()) {
                val a = JSONArray(historyFile.readText())
                for (i in 0 until a.length()) history += HistoryEntry.fromJson(a.getJSONObject(i))
            }
        }
    }

    fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(600)
            saveNow()
        }
    }

    fun saveNow() {
        val root = JSONObject()
            .put("workspaces", JSONArray(workspaces.map { it.toJson() }))
            .put("tabs", JSONArray(tabs.filter { !it.private }.map { it.toJson() }))
            .put("active", activeWorkspaceId)
            .put("selected", selectedTab?.takeIf { !it.private }?.id ?: "")
            .put("last", JSONObject().apply { lastSelected.forEach { (k, v) -> put(k, v ?: "") } })
        val hist = JSONArray(history.take(500).map { it.toJson() })
        scope.launch(Dispatchers.IO) {
            runCatching {
                val tmp = File(file.parentFile, "tabs.json.tmp")
                tmp.writeText(root.toString())
                tmp.renameTo(file)
                historyFile.writeText(hist.toString())
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Tabs
    // -----------------------------------------------------------------------------------------

    fun newTab(
        url: String,
        select: Boolean = true,
        private: Boolean = privateMode,
        parent: Tab? = null,
        workspaceId: String = activeWorkspaceId,
        fromHome: Boolean = false,
        external: Boolean = false,
    ): Tab {
        val tab = Tab(workspaceId = workspaceId, url = url, private = private, parentId = parent?.id, fromHome = fromHome, external = external)
        val idx = parent?.let { p -> tabs.indexOf(p).takeIf { it >= 0 }?.plus(1) } ?: tabs.size
        tabs.add(idx, tab)
        if (select) selectTab(tab)
        scheduleSave()
        return tab
    }

    fun selectTab(tab: Tab?) {
        val previous = selectedTab
        // Selecting a tab of the other kind switches the whole app into that mode.
        if (tab != null && tab.private != privateMode) {
            if (tab.private) lastNormalSelected = previous?.id else lastPrivateSelected = previous?.id
            privateMode = tab.private
        }
        if (tab != null && !tab.essential && tab.workspaceId != activeWorkspaceId) {
            activeWorkspaceId = tab.workspaceId
        }
        if (tab != null && tab !== previous) tab.showHome = false
        selectedTabId = tab?.id
        lastSelected[activeWorkspaceId] = tab?.id
        tab?.lastActive = System.currentTimeMillis()
        if (previous !== tab && !(settings.backgroundPlay && MediaHub.isPlaying(previous?.session))) previous?.session?.setActive(false)
        if (tab != null && !tab.isBlank) ensureSession(tab)
        tab?.session?.setActive(true)
        unloadIdleTabs()
        sessionVersion++
        scheduleSave()
    }

    fun closeTab(tab: Tab) {
        val wasSelected = tab.id == selectedTabId
        val siblings = when { tab.private -> privateTabs; tab.essential -> essentials; else -> tabsIn(tab.workspaceId) }
        val index = siblings.indexOf(tab)
        tab.session?.let { MediaHub.forget(it) }
        tab.session?.close()
        tab.session = null
        tabs.remove(tab)
        if (wasSelected) {
            val parent = tab.parentId?.let { p -> tabs.firstOrNull { it.id == p && it.private == tab.private } }
            val rest = if (tab.private) privateTabs else tabsIn(activeWorkspaceId)
            val next = parent?.takeIf { it.private || it.workspaceId == activeWorkspaceId || it.essential }
                ?: rest.getOrNull(index.coerceAtMost(rest.size - 1)).takeIf { !tab.essential }
                ?: rest.lastOrNull()
            selectTab(next)
        }
        scheduleSave()
    }

    /** Zen's "clear" under the separator: closes every unpinned tab in the workspace. */
    fun clearUnpinned(workspaceId: String) {
        tabsIn(workspaceId).filter { !it.pinned }.forEach { closeTab(it) }
    }

    /** Pinned tabs sit in the grid at the top of the sidebar, shared by every workspace. */
    fun togglePin(tab: Tab) {
        if (tab.private) return
        tab.essential = !tab.essential
        tab.pinned = false
        if (!tab.essential) tab.workspaceId = activeWorkspaceId
        scheduleSave()
    }

    fun moveToWorkspace(tab: Tab, workspaceId: String) {
        tab.essential = false
        tab.workspaceId = workspaceId
        if (tab.id == selectedTabId) {
            selectTab(tabsIn(activeWorkspaceId).lastOrNull())
        }
        scheduleSave()
    }

    fun duplicate(tab: Tab) {
        val copy = newTab(tab.url, select = true, parent = tab, private = tab.private)
        copy.title = tab.title
    }

    fun move(tab: Tab, toIndexInWorkspace: Int) {
        val list = tabsIn(tab.workspaceId)
        val target = list.getOrNull(toIndexInWorkspace) ?: return
        val from = tabs.indexOf(tab)
        val to = tabs.indexOf(target)
        if (from < 0 || to < 0 || from == to) return
        tabs.removeAt(from)
        tabs.add(to, tab)
        tab.pinned = target.pinned
        scheduleSave()
    }

    // -----------------------------------------------------------------------------------------
    // Workspaces
    // -----------------------------------------------------------------------------------------

    fun switchWorkspace(id: String) {
        if (privateMode) { activeWorkspaceId = id; switchPrivateMode(false); return }
        if (id == activeWorkspaceId) return
        activeWorkspaceId = id
        val remembered = lastSelected[id]?.let { r -> tabs.firstOrNull { it.id == r } }
        selectTab(remembered ?: tabsIn(id).lastOrNull())
    }

    fun addWorkspace(name: String, icon: String): Workspace {
        val ws = Workspace.new(name.ifBlank { "Space ${workspaces.size + 1}" }, icon)
        workspaces += ws
        switchWorkspace(ws.id)
        scheduleSave()
        return ws
    }

    fun editWorkspace(id: String, name: String, icon: String) {
        val i = workspaces.indexOfFirst { it.id == id }
        if (i >= 0) workspaces[i] = workspaces[i].copy(name = name.ifBlank { workspaces[i].name }, icon = icon)
        scheduleSave()
    }

    fun deleteWorkspace(id: String) {
        if (workspaces.size <= 1) return
        tabsIn(id).forEach { it.session?.close(); tabs.remove(it) }
        workspaces.removeAll { it.id == id }
        lastSelected.remove(id)
        if (activeWorkspaceId == id) {
            activeWorkspaceId = ""
            switchWorkspace(workspaces.first().id)
        }
        scheduleSave()
    }

    // -----------------------------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------------------------

    /** Address-bar submit: navigates [tab] (or a new tab) to the resolved input. */
    fun submit(input: String, tab: Tab? = selectedTab, newTab: Boolean = false, private: Boolean = privateMode) {
        val url = resolveInput(input) { settings.searchEngine.url(it) }
        if (url.isBlank()) return
        // A search from the homepage always starts a fresh history, so back returns to the homepage:
        //  - homepage behind an ordinary tab: that tab is replaced in place (count unchanged);
        //  - homepage behind a pinned tab: the pinned tab keeps its page, the search gets a new tab.
        if (tab != null && !newTab && tab.showHome) {
            tab.showHome = false
            if (tab.essential) { newTab(url, private = private, fromHome = true); return }
            replaceWithNewTab(tab, url, private)
            return
        }
        if (tab == null || newTab) {
            newTab(url, private = private, fromHome = true)
        } else {
            tab.showHome = false
            tab.url = url
            if (tab.session == null) ensureSession(tab) else tab.session?.loadUri(url)
            selectTab(tab)
        }
    }

    /** Swaps [old] for a brand-new tab at the same position (fresh history, same workspace). */
    private fun replaceWithNewTab(old: Tab, url: String, private: Boolean) {
        val idx = tabs.indexOf(old)
        val fresh = Tab(workspaceId = old.workspaceId, url = url, private = private, fromHome = true)
        old.session?.let { MediaHub.forget(it); it.close() }
        old.session = null
        if (idx >= 0) tabs[idx] = fresh else tabs.add(fresh)
        selectTab(fresh)
        scheduleSave()
    }

    fun goBack(): Boolean {
        val tab = selectedTab ?: return false
        if (tab.showHome) return false                      // already on the homepage: leave the app
        if (tab.canGoBack) { tab.session?.goBack(); return true }
        // Tab opened by a link from another tab: close it and return to that tab.
        val parent = tab.parentId?.let { p -> tabs.firstOrNull { it.id == p } }
        if (parent != null && !tab.essential) { closeTab(tab); selectTab(parent); return true }
        // First page of any tab: show the homepage (tab is kept), like Chrome's new-tab page.
        // Tabs opened by another app skip this so back returns to that app.
        if (!tab.external && !tab.isBlank) { tab.showHome = true; sessionVersion++; return true }
        return false
    }

    fun goForward() {
        val tab = selectedTab ?: return
        if (tab.showHome) { tab.showHome = false; sessionVersion++; return }   // forward from the homepage = the page again
        tab.session?.goForward()
    }

    fun reload() {
        val tab = selectedTab ?: return
        if (tab.loading) tab.session?.stop() else tab.session?.reload()
    }

    fun toggleDesktop(tab: Tab) {
        tab.desktop = !tab.desktop
        tab.session?.settings?.apply {
            userAgentMode = if (tab.desktop) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            viewportMode = if (tab.desktop) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        }
        tab.session?.reload()
    }

    fun findInPage(tab: Tab, text: String, backwards: Boolean = false, onResult: (Int, Int) -> Unit) {
        val finder = tab.session?.finder ?: return
        if (text.isEmpty()) { finder.clear(); onResult(0, 0); return }
        val flags = if (backwards) GeckoSession.FINDER_FIND_BACKWARDS else 0
        finder.find(text, flags).accept({ r -> onResult(r?.current ?: 0, r?.total ?: 0) }, { onResult(0, 0) })
    }

    fun clearFind(tab: Tab) { tab.session?.finder?.clear() }

    fun exitFullscreen() { selectedTab?.session?.exitFullScreen() }

    fun clearHistory() {
        history.clear()
        scheduleSave()
    }

    fun message(text: String) { _messages.tryEmit(text) }

    // -----------------------------------------------------------------------------------------
    // Sessions
    // -----------------------------------------------------------------------------------------

    private fun sessionSettings(tab: Tab) = GeckoSessionSettings.Builder()
        .usePrivateMode(tab.private)
        .useTrackingProtection(!isAllowlisted(tab.url))
        .userAgentMode(if (tab.desktop) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
        .viewportMode(if (tab.desktop) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
        .build()

    /** Creates and opens the tab's session if it is unloaded, restoring history when available. */
    fun ensureSession(tab: Tab): GeckoSession {
        tab.session?.let { return it }
        val session = GeckoSession(sessionSettings(tab))
        wire(tab, session)
        session.open(GeckoEngine.runtime)
        val state = tab.savedState?.let { GeckoSession.SessionState.fromString(it) }
        if (state != null) session.restoreState(state) else if (!tab.isBlank) session.loadUri(tab.url)
        tab.session = session
        tab.loaded = true
        return session
    }

    /** Zen-style tab unloading: keep at most [MAX_LIVE] sessions; the rest sleep with saved state. */
    private fun unloadIdleTabs() {
        val live = tabs.filter { it.session != null && it.id != selectedTabId && !it.private && !MediaHub.isPlaying(it.session) }
            .sortedByDescending { it.lastActive }
        live.drop(MAX_LIVE - 1).forEach { t ->
            t.session?.close()
            t.session = null
            t.loading = false
        }
    }

    private fun attachShield(tab: Tab, session: GeckoSession) {
        GeckoEngine.attachShield(session, object : WebExtension.MessageDelegate {
            override fun onMessage(nativeApp: String, message: Any, sender: WebExtension.MessageSender): GeckoResult<Any>? {
                val o = message as? JSONObject ?: return null
                if (o.optString("type") == "blocked") {
                    val count = o.optInt("count")
                    val delta = count - tab.adsBlocked
                    if (delta > 0) countBlocked(delta.toLong())
                    tab.adsBlocked = count
                }
                return null
            }
        })
    }

    private fun countBlocked(delta: Long) {
        pendingBlocked += delta
        if (blockedJob?.isActive == true) return
        blockedJob = scope.launch {
            delay(2000)
            val d = pendingBlocked
            pendingBlocked = 0
            repo.addBlocked(d)
        }
    }

    private fun wire(tab: Tab, session: GeckoSession) {
        attachShield(tab, session)

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                tab.loading = true
                tab.progress = 5
                tab.adsBlocked = 0
                tab.trackersBlocked = 0
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                tab.loading = false
                tab.progress = 100
            }
            override fun onProgressChange(session: GeckoSession, progress: Int) {
                tab.progress = progress
            }
            override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.ProgressDelegate.SecurityInformation) {
                tab.secure = securityInfo.isSecure
                tab.security = SiteSecurity(
                    isSecure = securityInfo.isSecure,
                    isException = securityInfo.isException,
                    host = securityInfo.host,
                    securityMode = securityInfo.securityMode,
                    mixedActive = securityInfo.mixedModeActive,
                    mixedPassive = securityInfo.mixedModePassive,
                    certificate = securityInfo.certificate,
                )
            }
            override fun onSessionStateChange(session: GeckoSession, sessionState: GeckoSession.SessionState) {
                if (!tab.private) {
                    tab.savedState = sessionState.toString()
                    scheduleSave()
                }
            }
        }

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean,
            ) {
                if (url == null || url == "about:blank" && tab.url.isNotBlank() && tab.url != "about:blank") return
                tab.url = url
                session.settings.useTrackingProtection = !isAllowlisted(url)
                scheduleSave()
            }
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) { tab.canGoBack = canGoBack }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) { tab.canGoForward = canGoForward }

            override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                val uri = request.uri
                val scheme = uri.substringBefore(':', "").lowercase()
                if (scheme in INTERNAL_SCHEMES) return null
                openExternally(uri)
                return GeckoResult.deny()
            }

            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession> {
                val child = newTab(uri, select = false, private = tab.private, parent = tab)
                val s = GeckoSession(sessionSettings(child))
                wire(child, s)
                child.session = s
                child.loaded = true
                // Gecko opens and loads it; select once it is open so the view attaches it.
                scope.launch(Dispatchers.Main) {
                    var waited = 0
                    while (!s.isOpen && waited < 2000) { delay(25); waited += 25 }
                    if (tabs.contains(child)) selectTab(child)
                }
                return GeckoResult.fromValue(s)
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                tab.title = title.orEmpty()
                if (!tab.private && !tab.isBlank && tab.url.startsWith("http")) recordHistory(tab.url, tab.title)
                scheduleSave()
            }
            override fun onCloseRequest(session: GeckoSession) { closeTab(tab) }
            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) { fullscreen = fullScreen }
            override fun onExternalResponse(session: GeckoSession, response: WebResponse) { download(response) }
            override fun onCrash(session: GeckoSession) { recover(tab) }
            override fun onKill(session: GeckoSession) { recover(tab) }
            override fun onContextMenu(session: GeckoSession, screenX: Int, screenY: Int, element: GeckoSession.ContentDelegate.ContextElement) {
                val isImage = element.type == GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE
                if (element.linkUri == null && !isImage) return
                contextMenu = ContextMenuRequest(tab, element.linkUri, element.srcUri, element.title ?: element.altText, isImage)
            }
        }

        session.scrollDelegate = object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                val dy = scrollY - tab.lastScrollY
                tab.lastScrollY = scrollY
                if (tab.id == selectedTabId) onScroll?.invoke(if (scrollY <= 0) Int.MIN_VALUE else dy)
            }
        }

        session.contentBlockingDelegate = object : ContentBlocking.Delegate {
            override fun onContentBlocked(session: GeckoSession, event: ContentBlocking.BlockEvent) {
                tab.trackersBlocked++
                countBlocked(1)
            }
        }

        session.promptDelegate = Prompts(this)
        session.mediaSessionDelegate = MediaHub.delegate { tab.host }
    }

    private fun recover(tab: Tab) {
        tab.session = null
        tab.loading = false
        if (tab.id == selectedTabId) {
            ensureSession(tab)
            sessionVersion++
        }
    }

    private fun recordHistory(url: String, title: String) {
        history.removeAll { it.url == url }
        history.add(0, HistoryEntry(url, title, System.currentTimeMillis()))
        if (history.size > 600) history.removeRange(500, history.size)
    }

    private fun openExternally(uri: String) {
        val intent = runCatching {
            if (uri.startsWith("intent:")) Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
            else Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        }.getOrNull() ?: return
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.component = null
        intent.selector = null
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            val fallback = if (uri.startsWith("intent:")) intent.getStringExtra("browser_fallback_url") else null
            if (fallback != null) selectedTab?.session?.loadUri(fallback)
            else message("No app can open this link")
        }
    }

    /** Streams an external response (download) into the public Downloads folder. */
    private fun download(response: WebResponse) {
        val body = response.body ?: return
        val disposition = response.headers["Content-Disposition"] ?: response.headers["content-disposition"]
        val mime = (response.headers["Content-Type"] ?: response.headers["content-type"])?.substringBefore(';')?.trim()
        val name = URLUtil.guessFileName(response.uri, disposition, mime)
        message("Downloading $name…")
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, name)
                            put(MediaStore.Downloads.MIME_TYPE, mime ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "")) ?: "application/octet-stream")
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }
                        val resolver = context.contentResolver
                        val item = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("insert")
                        resolver.openOutputStream(item)!!.use { out -> body.use { it.copyTo(out) } }
                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(item, values, null, null)
                    } else {
                        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                        File(dir, name).outputStream().use { out -> body.use { it.copyTo(out) } }
                    }
                }.isSuccess
            }
            message(if (ok) "Saved $name to Downloads" else "Download failed")
        }
    }

    companion object {
        const val MAX_LIVE = 5
        private val INTERNAL_SCHEMES = setOf("http", "https", "about", "data", "blob", "file", "resource", "moz-extension", "javascript", "view-source", "chrome")
    }
}
