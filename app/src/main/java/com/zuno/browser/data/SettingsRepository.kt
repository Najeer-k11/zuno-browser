package com.zuno.browser.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zuno.browser.ui.theme.DarkMode
import com.zuno.browser.ui.theme.Palette
import com.zuno.browser.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("zuno_settings")

enum class SearchEngine(val label: String, val template: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    STARTPAGE("Startpage", "https://www.startpage.com/do/search?q=%s"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=%s"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=%s"),
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s");

    fun url(query: String): String = template.replace("%s", android.net.Uri.encode(query))
}

data class BrowserSettings(
    val theme: ThemeSettings = ThemeSettings(),
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    /** Zuno Shield network + cosmetic blocking. */
    val adBlock: Boolean = true,
    /** Gecko Enhanced Tracking Protection: strict instead of standard. */
    val strictTracking: Boolean = true,
    /** Auto-reject cookie consent banners (Gecko cookie banner service). */
    val cookieBanners: Boolean = true,
    val httpsOnly: Boolean = false,
    val globalPrivacyControl: Boolean = true,
    /** Zen-style compact mode: the dock slides away while scrolling down. */
    val compactMode: Boolean = true,
    /** Keep web audio/video playing with Zuno in the background (YouTube, YouTube Music, …). */
    val backgroundPlay: Boolean = true,
    val javaScript: Boolean = true,
    /** Hosts (no www.) where blocking is off. */
    val allowlist: Set<String> = emptySet(),
    val blockedTotal: Long = 0,
    val onboarded: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object K {
        val dynamic = booleanPreferencesKey("dynamic_color")
        val palette = stringPreferencesKey("palette")
        val dark = stringPreferencesKey("dark_mode")
        val amoled = booleanPreferencesKey("amoled")
        val engine = stringPreferencesKey("search_engine")
        val adBlock = booleanPreferencesKey("ad_block")
        val strict = booleanPreferencesKey("strict_tracking")
        val banners = booleanPreferencesKey("cookie_banners")
        val https = booleanPreferencesKey("https_only")
        val gpc = booleanPreferencesKey("gpc")
        val compact = booleanPreferencesKey("compact_mode")
        val bgPlay = booleanPreferencesKey("background_play")
        val js = booleanPreferencesKey("javascript")
        val allow = stringSetPreferencesKey("allowlist")
        val blocked = longPreferencesKey("blocked_total")
        val onboarded = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<BrowserSettings> = context.dataStore.data.map { p -> p.toSettings() }

    private fun Preferences.toSettings() = BrowserSettings(
        theme = ThemeSettings(
            dynamicColor = this[K.dynamic] ?: true,
            palette = this[K.palette]?.let { n -> Palette.entries.firstOrNull { it.name == n } } ?: Palette.CONCEPT,
            darkMode = this[K.dark]?.let { n -> DarkMode.entries.firstOrNull { it.name == n } } ?: DarkMode.SYSTEM,
            amoledBlack = this[K.amoled] ?: false,
        ),
        searchEngine = this[K.engine]?.let { n -> SearchEngine.entries.firstOrNull { it.name == n } } ?: SearchEngine.DUCKDUCKGO,
        adBlock = this[K.adBlock] ?: true,
        strictTracking = this[K.strict] ?: true,
        cookieBanners = this[K.banners] ?: true,
        httpsOnly = this[K.https] ?: false,
        globalPrivacyControl = this[K.gpc] ?: true,
        compactMode = this[K.compact] ?: true,
        backgroundPlay = this[K.bgPlay] ?: true,
        javaScript = this[K.js] ?: true,
        allowlist = this[K.allow] ?: emptySet(),
        blockedTotal = this[K.blocked] ?: 0L,
        onboarded = this[K.onboarded] ?: false,
    )

    suspend fun update(transform: (BrowserSettings) -> BrowserSettings) {
        context.dataStore.edit { p ->
            val s = transform(p.toSettings())
            p[K.dynamic] = s.theme.dynamicColor
            p[K.palette] = s.theme.palette.name
            p[K.dark] = s.theme.darkMode.name
            p[K.amoled] = s.theme.amoledBlack
            p[K.engine] = s.searchEngine.name
            p[K.adBlock] = s.adBlock
            p[K.strict] = s.strictTracking
            p[K.banners] = s.cookieBanners
            p[K.https] = s.httpsOnly
            p[K.gpc] = s.globalPrivacyControl
            p[K.compact] = s.compactMode
            p[K.bgPlay] = s.backgroundPlay
            p[K.js] = s.javaScript
            p[K.allow] = s.allowlist
            p[K.blocked] = s.blockedTotal
            p[K.onboarded] = s.onboarded
        }
    }

    suspend fun addBlocked(delta: Long) {
        if (delta <= 0) return
        context.dataStore.edit { p -> p[K.blocked] = (p[K.blocked] ?: 0L) + delta }
    }
}
