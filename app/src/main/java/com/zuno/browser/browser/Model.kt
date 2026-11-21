package com.zuno.browser.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import org.mozilla.geckoview.GeckoSession
import java.util.UUID

/** Zen-style workspace: a named set of pinned + regular tabs, badged with a [com.zuno.browser.ui.ZunoIcons] key. */
data class Workspace(val id: String, val name: String, val icon: String) {
    fun toJson(): JSONObject = JSONObject().put("id", id).put("name", name).put("icon", icon)

    companion object {
        fun fromJson(o: JSONObject) = Workspace(
            o.getString("id"),
            o.getString("name"),
            o.optString("icon").ifBlank { legacyIcon(o.optString("name")) },
        )
        fun new(name: String, icon: String) = Workspace(UUID.randomUUID().toString(), name, icon)

        /** Pre-icon builds stored an emoji; map the two defaults, everything else gets the default icon. */
        private fun legacyIcon(name: String) = when (name) {
            "Personal" -> "leaf"
            "Work" -> "briefcase"
            else -> com.zuno.browser.ui.ZunoIcons.DEFAULT
        }
    }
}

/**
 * A tab. Observable fields drive the UI; [session] is the live Gecko session, or null while the
 * tab is unloaded (Zen-style tab unloading keeps only a handful of sessions alive and restores the
 * rest from [savedState] on demand).
 */
class Tab(
    val id: String = UUID.randomUUID().toString(),
    workspaceId: String,
    url: String = "",
    title: String = "",
    pinned: Boolean = false,
    essential: Boolean = false,
    val private: Boolean = false,
    var savedState: String? = null,
    var parentId: String? = null,
    /** Started from the homepage / new-tab screen: back on its first page returns to the homepage. */
    var fromHome: Boolean = false,
    /** Opened by another app (VIEW intent): back on its first page returns to that app. */
    var external: Boolean = false,
) {
    var workspaceId by mutableStateOf(workspaceId)
    var url by mutableStateOf(url)
    var title by mutableStateOf(title)
    var pinned by mutableStateOf(pinned)
    var essential by mutableStateOf(essential)

    var progress by mutableIntStateOf(100)
    var loading by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var secure by mutableStateOf(false)
    var security by mutableStateOf<SiteSecurity?>(null)
    var desktop by mutableStateOf(false)
    /** Requests blocked by Zuno Shield on the current page. */
    var adsBlocked by mutableIntStateOf(0)
    /** Trackers blocked by Gecko ETP on the current page. */
    var trackersBlocked by mutableIntStateOf(0)
    var loaded by mutableStateOf(false)
    /** Showing the homepage "behind" its first page (after pressing back), like Chrome's new-tab page. */
    var showHome by mutableStateOf(false)

    var session: GeckoSession? = null
    var lastActive: Long = System.currentTimeMillis()
    var lastScrollY = 0

    val isBlank get() = url.isBlank() || url == "about:blank"
    val host: String get() = hostOf(url)
    val displayTitle: String get() = title.takeIf { it.isNotBlank() && it != url } ?: host.ifBlank { "New tab" }
    val totalBlocked get() = adsBlocked + trackersBlocked

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("ws", workspaceId).put("url", url).put("title", title)
        .put("pinned", pinned).put("essential", essential).put("state", savedState ?: JSONObject.NULL)
        .put("parent", parentId ?: JSONObject.NULL)
        .put("fromHome", fromHome)
        .put("external", external)

    companion object {
        fun fromJson(o: JSONObject) = Tab(
            id = o.getString("id"),
            workspaceId = o.getString("ws"),
            url = o.optString("url"),
            title = o.optString("title"),
            pinned = o.optBoolean("pinned"),
            essential = o.optBoolean("essential"),
            savedState = o.optString("state").takeIf { !o.isNull("state") && it.isNotBlank() },
            parentId = o.optString("parent").takeIf { !o.isNull("parent") && it.isNotBlank() },
            fromHome = o.optBoolean("fromHome"),
            external = o.optBoolean("external"),
        )
    }
}

/** Snapshot of Gecko's security info for the tab's current page. */
data class SiteSecurity(
    val isSecure: Boolean,
    /** The user (or a policy) accepted a certificate error for this site. */
    val isException: Boolean,
    val host: String,
    /** 0 unknown, 1 identified (DV), 2 verified (EV) - GeckoSession SECURITY_MODE_*. */
    val securityMode: Int,
    /** Gecko CONTENT_* for insecure scripts/frames (active) and images/media (passive). */
    val mixedActive: Int,
    val mixedPassive: Int,
    val certificate: java.security.cert.X509Certificate?,
)

data class HistoryEntry(val url: String, val title: String, val time: Long) {
    fun toJson(): JSONObject = JSONObject().put("u", url).put("t", title).put("ts", time)

    companion object {
        fun fromJson(o: JSONObject) = HistoryEntry(o.getString("u"), o.optString("t"), o.optLong("ts"))
    }
}

fun hostOf(url: String): String = runCatching {
    android.net.Uri.parse(url).host.orEmpty().removePrefix("www.")
}.getOrDefault("")

/** First visible letter of a host, used for the cookie-shaped site badges. */
fun monogram(text: String): String =
    text.removePrefix("www.").removePrefix("m.").removePrefix("mobile.").firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "•"

private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
private val HOSTLIKE = Regex("^([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(:\\d+)?(/.*)?$")
private val IPLIKE = Regex("^(\\d{1,3}\\.){3}\\d{1,3}(:\\d+)?(/.*)?$")

/** Turns address-bar input into a URL: explicit URLs pass, host-like gets https://, else search. */
fun resolveInput(input: String, search: (String) -> String): String {
    val t = input.trim()
    if (t.isEmpty()) return ""
    if (t.contains(' ')) return search(t)
    if (t.startsWith("about:") || t.startsWith("view-source:")) return t
    if (SCHEME.containsMatchIn(t) && (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("file:") || t.startsWith("data:"))) return t
    if (t.startsWith("localhost")) return "http://$t"
    if (HOSTLIKE.matches(t) || IPLIKE.matches(t)) return "https://$t"
    return search(t)
}
