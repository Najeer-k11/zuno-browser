package com.zuno.browser.ui.browser

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zuno.browser.browser.BrowserStore
import com.zuno.browser.browser.Tab
import com.zuno.browser.browser.Workspace
import com.zuno.browser.data.BrowserSettings
import com.zuno.browser.engine.GeckoEngine
import com.zuno.browser.ui.BigActionButton
import com.zuno.browser.ui.glass
import com.zuno.browser.ui.isAppDark
import com.zuno.browser.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoView
import kotlin.math.roundToInt

/** Workspace sheet target: null = closed, [creating] = new. */
private data class WorkspaceEdit(val existing: Workspace?)

@Composable
fun ZunoRoot(
    store: BrowserStore,
    settings: BrowserSettings,
    onUpdate: ((BrowserSettings) -> BrowserSettings) -> Unit,
) {
    if (!settings.onboarded) {
        com.zuno.browser.ui.onboarding.Onboarding(settings, onUpdate) { onUpdate { it.copy(onboarded = true) } }
        return
    }
    BrowserScreen(store, settings, onUpdate)
}

@Composable
fun BrowserScreen(
    store: BrowserStore,
    settings: BrowserSettings,
    onUpdate: ((BrowserSettings) -> BrowserSettings) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dark = isAppDark()

    var showSidebar by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf<Tab?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSiteInfo by remember { mutableStateOf(false) }
    var revealOrigin by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    val reveal = remember { Animatable(1f) }
    var seenPrivate by remember { mutableStateOf(store.privateMode) }
    LaunchedEffect(store.privateMode) {
        if (seenPrivate == store.privateMode) return@LaunchedEffect
        seenPrivate = store.privateMode
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    var showSettings by remember { mutableStateOf(false) }
    var tabActions by remember { mutableStateOf<Tab?>(null) }
    var wsEdit by remember { mutableStateOf<WorkspaceEdit?>(null) }
    var finding by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findResult by remember { mutableStateOf(0 to 0) }

    val tab = store.selectedTab
    val fullscreen = store.fullscreen
    val shieldOn = settings.adBlock
    val showStart = tab == null || tab.isBlank || tab.showHome

    // Gecko follows the in-app theme for prefers-color-scheme.
    LaunchedEffect(dark) {
        GeckoEngine.runtime.settings.setPreferredColorScheme(if (dark) GeckoRuntimeSettings.COLOR_SCHEME_DARK else GeckoRuntimeSettings.COLOR_SCHEME_LIGHT)
    }

    // Immersive mode for fullscreen video.
    LaunchedEffect(fullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val ctl = WindowCompat.getInsetsController(window, window.decorView)
        if (fullscreen) {
            ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctl.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            ctl.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Dock visibility (Zen compact mode): 1 = shown, 0 = tucked away below the screen.
    val dockShown = remember { Animatable(1f) }
    val showDock = { scope.launch { dockShown.animateTo(1f, spring(dampingRatio = .72f, stiffness = 420f)) } }
    val hideDock = { scope.launch { dockShown.animateTo(0f, spring(dampingRatio = .9f, stiffness = 380f)) } }
    DisposableEffect(settings.compactMode) {
        store.onScroll = { dy ->
            when {
                !settings.compactMode -> Unit
                dy == Int.MIN_VALUE -> if (dockShown.targetValue < 1f) showDock()
                dy > 14 -> if (dockShown.targetValue > 0f) hideDock()
                dy < -14 -> if (dockShown.targetValue < 1f) showDock()
            }
        }
        onDispose { store.onScroll = null }
    }
    LaunchedEffect(tab?.id, tab?.url, settings.compactMode) { showDock() }

    // Media started but notifications are off: ask now, so the media controls can appear.
    val notifPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        com.zuno.browser.media.MediaHub.permissionHandled()
        if (!granted) store.message("Allow notifications to get media controls")
    }
    val needsPermission = com.zuno.browser.media.MediaHub.needsNotificationPermission
    LaunchedEffect(needsPermission) {
        if (needsPermission && android.os.Build.VERSION.SDK_INT >= 33) notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { store.messages.collect { snackbar.showSnackbar(it) } }

    // GeckoView instance (kept for the life of the screen).
    var geckoView by remember { mutableStateOf<GeckoView?>(null) }
    // GeckoView's SurfaceView is destroyed while the app is in the background; on return Gecko
    // only repainted part of it (the rest stayed black). Normally we pause the session with the
    // activity, which forces a full composite on resume. A tab playing media is kept active instead
    // (background play), and gets re-attached to the view on return to force the repaint.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val currentSettings by androidx.compose.runtime.rememberUpdatedState(settings)
    DisposableEffect(lifecycleOwner) {
        var pausedOnStop: org.mozilla.geckoview.GeckoSession? = null
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    val s = store.selectedTab?.session
                    val keepPlaying = currentSettings.backgroundPlay && com.zuno.browser.media.MediaHub.isPlaying(s)
                    if (s != null && !keepPlaying) {
                        if (!currentSettings.backgroundPlay) com.zuno.browser.media.MediaHub.pause()
                        s.setActive(false)
                        pausedOnStop = s
                    } else pausedOnStop = null
                }
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    val s = store.selectedTab?.session
                    val view = geckoView
                    if (s != null && pausedOnStop === s) {
                        s.setActive(true)
                    } else if (s != null && view != null && view.session === s) {
                        view.releaseSession()
                        view.setSession(s)
                    }
                    pausedOnStop = null
                    view?.post { view.requestLayout(); view.invalidate() }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navInset = WindowInsets.navigationBars
    val reservePx = with(density) { BAR_RESERVE.roundToPx() + navInset.getBottom(this) }
    LaunchedEffect(geckoView, reservePx, fullscreen) { geckoView?.setDynamicToolbarMaxHeight(if (fullscreen) 0 else reservePx) }
    LaunchedEffect(geckoView) {
        androidx.compose.runtime.snapshotFlow { dockShown.value }.collect { v ->
            // Clipping is relative to the dynamic-toolbar max height: 0 = dock fully shown,
            // -max = dock fully hidden (fixed-bottom page elements drop to the screen edge).
            geckoView?.setVerticalClipping(-(reservePx * (1f - v)).roundToInt())
        }
    }

    fun toggleShieldForSite(t: Tab) {
        val host = t.host
        if (host.isBlank()) return
        val allowed = store.isAllowlisted(t.url)
        onUpdate { s -> s.copy(allowlist = if (allowed) s.allowlist.filterNot { host == it || host.endsWith(".$it") }.toSet() else s.allowlist + host) }
        store.message(if (allowed) "Shield on for $host" else "Shield paused for $host")
        scope.launch { kotlinx.coroutines.delay(250); t.session?.reload() }
    }

    BackHandler {
        when {
            showSiteInfo -> showSiteInfo = false
            showSettings -> showSettings = false
            searchOpen -> searchOpen = false
            finding -> { finding = false; tab?.let { store.clearFind(it) } }
            showSidebar -> showSidebar = false
            fullscreen -> store.exitFullscreen()
            store.goBack() -> Unit
            else -> activity?.moveTaskToBack(true)
        }
    }

    Box(Modifier.fillMaxSize().background(cs.background)) {
        // 1. Web content.
        val bgArgb = cs.background.toArgb()
        Box(Modifier.fillMaxSize().then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.statusBars))) {
            AndroidView(
                factory = { ctx -> GeckoView(ctx).also { geckoView = it } },
                update = { view ->
                    @Suppress("UNUSED_EXPRESSION") store.sessionVersion
                    val s = tab?.session
                    if (s != null && s.isOpen) {
                        if (view.session !== s) {
                            view.setSession(s)
                            s.compositorController.setClearColor(bgArgb)
                        }
                    } else if (view.session != null && (s == null || view.session?.isOpen == false)) {
                        view.releaseSession()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 2. Start page for blank tabs / empty workspaces.
        AnimatedVisibility(showStart, enter = fadeIn(tween(220)), exit = fadeOut(tween(160))) {
            StartPage(
                store = store,
                blockedTotal = settings.blockedTotal,
                shieldOn = shieldOn,
                // The homepage reuses the tab behind it, except pinned tabs (those get a new tab).
                onSearch = { search = tab?.takeIf { !it.essential }; searchOpen = true },
                onOpen = { store.selectTab(it) },
                onTabActions = { tabActions = it },
            )
        }

        // 3. Wavy progress under the status bar.
        val loading = tab?.loading == true && !showStart && !fullscreen
        val progress by animateFloatAsState((tab?.progress ?: 0) / 100f, tween(260), label = "progress")
        AnimatedVisibility(
            loading,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 6.dp),
        ) {
            LinearWavyProgressIndicator(progress = { progress.coerceAtLeast(.05f) }, modifier = Modifier.fillMaxWidth().height(10.dp))
        }

        // 4. Dock / find bar.
        if (!fullscreen) {
            val hiddenPx = with(density) { (BAR_HEIGHT + 24.dp).roundToPx() + navInset.getBottom(this) }
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .offset { IntOffset(0, ((1f - dockShown.value) * hiddenPx).roundToInt()) }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = BAR_GAP),
            ) {
                AnimatedContent(
                    finding,
                    transitionSpec = { (fadeIn(tween(180)) + slideInVertically(spring(dampingRatio = .8f, stiffness = 420f)) { it / 2 }) togetherWith fadeOut(tween(120)) },
                    label = "dockMode",
                ) { isFinding ->
                    if (isFinding && tab != null) {
                        FindBar(
                            query = findQuery,
                            onQuery = { q -> findQuery = q; store.findInPage(tab, q) { c, t -> findResult = c to t } },
                            current = findResult.first, total = findResult.second,
                            onPrev = { store.findInPage(tab, findQuery, backwards = true) { c, t -> findResult = c to t } },
                            onNext = { store.findInPage(tab, findQuery) { c, t -> findResult = c to t } },
                            onClose = { finding = false; store.clearFind(tab) },
                        )
                    } else {
                        BrowserDock(
                            tab = tab?.takeIf { !it.showHome },
                            tabCount = store.modeTabs.size,
                            shieldOn = shieldOn && tab?.let { !store.isAllowlisted(it.url) } != false,
                            onTabs = { showSidebar = true },
                            onAddress = { search = tab?.takeIf { (!it.isBlank && !it.showHome) || (it.showHome && !it.essential) }; searchOpen = true },
                            onShield = { if (tab != null && !tab.isBlank) showSiteInfo = true else showMenu = true },
                            onMenu = { showMenu = true },
                            onNewTab = { search = null; searchOpen = true },
                            onSwipeTab = { dir ->
                                val list = store.modeTabs
                                val i = list.indexOf(tab)
                                list.getOrNull(if (i < 0) 0 else i + dir)?.let { store.selectTab(it) }
                            },
                        )
                    }
                }
            }
        }

        // 5. Snackbars sit above the dock.
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = BAR_RESERVE + 8.dp, start = 16.dp, end = 16.dp),
        )

        // 6. Zen sidebar.
        SidebarOverlay(
            open = showSidebar,
            onClose = { showSidebar = false },
        ) {
            TabSidebar(
                store = store,
                onSelect = { store.selectTab(it); showSidebar = false },
                onNewTab = { search = null; searchOpen = true; showSidebar = false },
                onTabActions = { tabActions = it },
                onAddWorkspace = { wsEdit = WorkspaceEdit(null) },
                onEditWorkspace = { wsEdit = WorkspaceEdit(it) },
                onSettings = { showSettings = true },
                onPrivateChipAt = { revealOrigin = it },
            )
        }

        // 6b. Private-mode reveal: a soft circle of the new mode's colour expands from the mask chip.
        if (reveal.value < 1f) {
            val tint = cs.primary
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val o = revealOrigin ?: androidx.compose.ui.geometry.Offset(size.width / 2f, size.height)
                val maxR = kotlin.math.hypot(maxOf(o.x, size.width - o.x), maxOf(o.y, size.height - o.y))
                val p = reveal.value
                drawCircle(tint.copy(alpha = .32f * (1f - p)), radius = maxR * p, center = o)
                drawCircle(tint.copy(alpha = .5f * (1f - p)), radius = maxR * p, center = o, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx() * (1f - p)))
            }
        }

        // 7. Address / new tab.
        AnimatedVisibility(
            searchOpen,
            enter = slideInVertically(spring(dampingRatio = .82f, stiffness = 380f)) { it / 5 } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it / 6 } + fadeOut(tween(180)),
        ) {
            SearchOverlay(store, editing = search, onDismiss = { searchOpen = false })
        }

        // 8. Settings slides in from the right.
        AnimatedVisibility(
            showSettings,
            enter = slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { it },
            exit = slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { it },
        ) {
            SettingsScreen(store, settings, onUpdate, onBack = { showSettings = false })
        }
    }

    // Sheets and dialogs.
    if (showMenu) {
        MenuSheet(
            store = store,
            tab = tab,
            shieldOn = shieldOn,
            onToggleShieldForSite = { tab?.let { toggleShieldForSite(it) } },
            onFind = { showMenu = false; findQuery = ""; findResult = 0 to 0; finding = true },
            onSettings = { showMenu = false; showSettings = true },
            onNewPrivate = { showMenu = false; store.switchPrivateMode(!store.privateMode) },
            onDismiss = { showMenu = false },
        )
    }
    if (showSiteInfo && tab != null) {
        SiteInfoSheet(
            store = store,
            tab = tab,
            shieldOn = shieldOn,
            onToggleShield = { toggleShieldForSite(tab) },
            onSettings = { showSiteInfo = false; showSettings = true },
            onDismiss = { showSiteInfo = false },
        )
    }
    tabActions?.let { t -> TabActionsSheet(store, t) { tabActions = null } }
    wsEdit?.let { e ->
        WorkspaceSheet(
            existing = e.existing,
            canDelete = store.workspaces.size > 1,
            onSave = { name, icon ->
                if (e.existing == null) store.addWorkspace(name, icon) else store.editWorkspace(e.existing.id, name, icon)
                wsEdit = null
            },
            onDelete = { e.existing?.let { store.deleteWorkspace(it.id) }; wsEdit = null },
            onDismiss = { wsEdit = null },
        )
    }
    store.contextMenu?.let { req -> ContextMenuSheet(store, req) { store.contextMenu = null } }
    store.prompt?.let { p -> PromptHost(p) { if (store.prompt === p) store.prompt = null } }
}

/** Scrim + glass panel sliding in from the left, spring-driven; drag the scrim left to close. */
@Composable
private fun SidebarOverlay(open: Boolean, onClose: () -> Unit, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dark = isAppDark()
    val progress = remember { Animatable(0f) }
    var drag by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(open) {
        drag = 0
        progress.animateTo(if (open) 1f else 0f, if (open) spring(dampingRatio = .8f, stiffness = 380f) else spring(dampingRatio = 1f, stiffness = 520f))
    }
    if (!open && progress.value == 0f) return
    val scrimAlpha by animateFloatAsState(if (open) .42f else 0f, tween(260), label = "scrim")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelWidth = minOf(maxWidth * .88f, 420.dp)
        val widthPx = with(LocalDensity.current) { panelWidth.toPx() }
        Box(
            Modifier.fillMaxSize().background(cs.scrim.copy(alpha = scrimAlpha))
                .pointerInput(Unit) { detectTapGestures { onClose() } }
                .pointerInput("drag") {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (drag < -widthPx * .25f) onClose()
                            else scope.launch { drag = 0 }
                        },
                    ) { change, amount ->
                        change.consume()
                        drag = (drag + amount.roundToInt()).coerceIn(-widthPx.roundToInt(), 0)
                    }
                },
        )
        val panelShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
        Box(
            Modifier.width(panelWidth).fillMaxHeight()
                .graphicsLayer { translationX = -widthPx * (1f - progress.value) + drag }
                .clip(panelShape)
                // Opaque base: the web page must not bleed through the tab list.
                .background(if (dark) cs.surfaceContainer else cs.surfaceContainerLow)
                .glass(panelShape, if (dark) cs.surfaceContainer.copy(alpha = .97f) else cs.surfaceContainerLow.copy(alpha = .97f), .88f, shadowElevation = 24.dp),
        ) { content() }
    }
}

