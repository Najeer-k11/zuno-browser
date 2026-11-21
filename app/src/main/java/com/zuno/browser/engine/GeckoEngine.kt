package com.zuno.browser.engine

import android.content.Context
import android.util.Log
import com.zuno.browser.data.BrowserSettings
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoPreferenceController
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

/**
 * Owns the single [GeckoRuntime] of the process (Gecko allows exactly one) and Zuno Shield,
 * the bundled ad-blocking WebExtension.
 *
 * Blocking is layered:
 *  1. Gecko Enhanced Tracking Protection with the ad, analytics, social, cryptomining and
 *     fingerprinting categories (Disconnect lists, shipped and updated by Gecko itself).
 *  2. Zuno Shield: network blocking against a bundled host list + cosmetic hiding of ad slots.
 *  3. Gecko's cookie-banner service, auto-rejecting consent popups.
 * Per-site allowlisting turns off (1) per session and (2) via the extension's config channel.
 */
object GeckoEngine {
    private const val TAG = "ZunoEngine"
    private const val SHIELD_ID = "shield@zuno.browser"
    private const val SHIELD_URI = "resource://android/assets/zunoblock/"
    const val NATIVE_APP = "zunoshield"

    lateinit var runtime: GeckoRuntime
        private set

    var shield: WebExtension? = null
        private set
    private var shieldPort: WebExtension.Port? = null
    private var lastSettings: BrowserSettings? = null

    /** Called when Shield (re)attaches, so the store can wire existing sessions to it. */
    var onShieldReady: ((WebExtension) -> Unit)? = null

    val isReady get() = ::runtime.isInitialized

    fun init(context: Context, settings: BrowserSettings) {
        if (isReady) return
        val cb = ContentBlocking.Settings.Builder()
            .antiTracking(trackingCategories(settings.strictTracking))
            .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
            .enhancedTrackingProtectionLevel(
                if (settings.strictTracking) ContentBlocking.EtpLevel.STRICT else ContentBlocking.EtpLevel.DEFAULT,
            )
            .build()
        val rs = GeckoRuntimeSettings.Builder()
            .contentBlocking(cb)
            .javaScriptEnabled(settings.javaScript)
            .allowInsecureConnections(if (settings.httpsOnly) GeckoRuntimeSettings.HTTPS_ONLY else GeckoRuntimeSettings.ALLOW_ALL)
            .preferredColorScheme(GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM)
            .consoleOutput(false)
            .aboutConfigEnabled(true)
            .build()
        runtime = GeckoRuntime.create(context.applicationContext, rs)
        runtime.settings.setGlobalPrivacyControl(settings.globalPrivacyControl)
        lastSettings = settings
        setCookieBannerMode(settings.cookieBanners)
        installExtensionPrompt()
        installShield()
    }

    private fun trackingCategories(strict: Boolean): Int {
        val base = ContentBlocking.AntiTracking.AD or ContentBlocking.AntiTracking.ANALYTIC or
            ContentBlocking.AntiTracking.SOCIAL or ContentBlocking.AntiTracking.CRYPTOMINING or
            ContentBlocking.AntiTracking.FINGERPRINTING
        return if (strict) ContentBlocking.AntiTracking.STRICT or base else base
    }

    /** Pushes setting changes into the live runtime and Shield. */
    fun apply(settings: BrowserSettings) {
        if (!isReady) return
        val old = lastSettings
        lastSettings = settings
        val rs = runtime.settings
        if (old?.strictTracking != settings.strictTracking) {
            rs.contentBlocking.setAntiTracking(trackingCategories(settings.strictTracking))
            rs.contentBlocking.setEnhancedTrackingProtectionLevel(
                if (settings.strictTracking) ContentBlocking.EtpLevel.STRICT else ContentBlocking.EtpLevel.DEFAULT,
            )
        }
        if (old?.javaScript != settings.javaScript) rs.setJavaScriptEnabled(settings.javaScript)
        if (old?.httpsOnly != settings.httpsOnly) {
            rs.setAllowInsecureConnections(if (settings.httpsOnly) GeckoRuntimeSettings.HTTPS_ONLY else GeckoRuntimeSettings.ALLOW_ALL)
        }
        if (old?.globalPrivacyControl != settings.globalPrivacyControl) rs.setGlobalPrivacyControl(settings.globalPrivacyControl)
        if (old?.cookieBanners != settings.cookieBanners) setCookieBannerMode(settings.cookieBanners)
        if (old?.adBlock != settings.adBlock || old.allowlist != settings.allowlist || old.backgroundPlay != settings.backgroundPlay) pushShieldConfig()
    }

    private fun setCookieBannerMode(on: Boolean) {
        // 1 = reject all, 0 = off. Private-browsing mode follows the same choice.
        val mode = if (on) 1 else 0
        runCatching {
            GeckoPreferenceController.setGeckoPref("cookiebanners.service.mode", mode, GeckoPreferenceController.PREF_BRANCH_USER)
            GeckoPreferenceController.setGeckoPref("cookiebanners.service.mode.privateBrowsing", mode, GeckoPreferenceController.PREF_BRANCH_USER)
        }.onFailure { Log.w(TAG, "cookie banner pref", it) }
    }

    private fun installShield() {
        runtime.webExtensionController.ensureBuiltIn(SHIELD_URI, SHIELD_ID).accept(
            { ext ->
                if (ext == null) return@accept
                shield = ext
                ext.setMessageDelegate(object : WebExtension.MessageDelegate {
                    override fun onConnect(port: WebExtension.Port) {
                        shieldPort = port
                        port.setDelegate(object : WebExtension.PortDelegate {
                            override fun onPortMessage(message: Any, port: WebExtension.Port) {
                                if ((message as? JSONObject)?.optString("type") == "ready") pushShieldConfig()
                            }
                            override fun onDisconnect(port: WebExtension.Port) {
                                if (shieldPort === port) shieldPort = null
                            }
                        })
                        pushShieldConfig()
                    }
                }, NATIVE_APP)
                onShieldReady?.invoke(ext)
            },
            { e -> Log.e(TAG, "Zuno Shield failed to install", e) },
        )
    }

    private fun pushShieldConfig() {
        val s = lastSettings ?: return
        val msg = JSONObject()
            .put("type", "config")
            .put("enabled", s.adBlock)
            .put("allow", JSONArray(s.allowlist.toList()))
            .put("bgPlay", s.backgroundPlay)
        runCatching { shieldPort?.postMessage(msg) }
    }

    /** Auto-accept install prompts for extensions the user explicitly asked to install. */
    private fun installExtensionPrompt() {
        runtime.webExtensionController.setPromptDelegate(object : WebExtensionController.PromptDelegate {
            override fun onInstallPromptRequest(
                extension: WebExtension,
                permissions: Array<out String>,
                origins: Array<out String>,
                dataCollectionPermissions: Array<out String>,
            ): GeckoResult<WebExtension.PermissionPromptResponse> =
                GeckoResult.fromValue(WebExtension.PermissionPromptResponse(true, false, false))
        })
    }

    /** One-tap install of uBlock Origin from addons.mozilla.org, for users wanting full filter lists. */
    fun installUBlockOrigin(onDone: (Result<String>) -> Unit) {
        runtime.webExtensionController
            .install("https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi")
            .accept(
                { ext -> onDone(Result.success(ext?.metaData?.name ?: "uBlock Origin")) },
                { e -> onDone(Result.failure(e ?: Exception("Install failed"))) },
            )
    }

    fun listUserExtensions(onResult: (List<WebExtension>) -> Unit) {
        runtime.webExtensionController.list().accept(
            { list -> onResult(list.orEmpty().filter { it.id != SHIELD_ID }) },
            { onResult(emptyList()) },
        )
    }

    fun uninstall(ext: WebExtension, onDone: () -> Unit) {
        runtime.webExtensionController.uninstall(ext).accept({ onDone() }, { onDone() })
    }

    /** Wire a session to Shield so its content script can report blocked counts. */
    fun attachShield(session: GeckoSession, delegate: WebExtension.MessageDelegate) {
        val ext = shield ?: return
        session.webExtensionController.setMessageDelegate(ext, delegate, NATIVE_APP)
    }

    /** Cookies, storage and cache for one site (and its subdomains). */
    fun clearSiteData(host: String, onDone: () -> Unit) {
        val base = host.removePrefix("www.")
        runtime.storageController.clearDataFromBaseDomain(base, StorageController.ClearFlags.ALL).accept({ onDone() }, { onDone() })
    }

    fun clearBrowsingData(onDone: () -> Unit) {
        runtime.storageController.clearData(StorageController.ClearFlags.ALL).accept({ onDone() }, { onDone() })
    }
}
