package com.zuno.browser

import android.app.Application
import com.zuno.browser.browser.BrowserStore
import com.zuno.browser.data.BrowserSettings
import com.zuno.browser.data.SettingsRepository
import com.zuno.browser.engine.GeckoEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ZunoApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var repo: SettingsRepository
        private set
    lateinit var settings: StateFlow<BrowserSettings>
        private set
    lateinit var store: BrowserStore
        private set

    override fun onCreate() {
        super.onCreate()
        // Gecko spawns child processes that re-enter Application.onCreate; only init in the main one.
        if (currentProcess() != packageName) return
        repo = SettingsRepository(this)
        com.zuno.browser.media.MediaHub.init(this)
        // Settings are tiny; reading them synchronously once avoids a flash of wrong theme and
        // lets the runtime start with the right privacy settings.
        val initial = runBlocking { repo.settings.first() }
        settings = repo.settings.stateIn(appScope, SharingStarted.Eagerly, initial)
        GeckoEngine.init(this, initial)
        store = BrowserStore(this, repo)
        store.updateSettings(initial)
        appScope.launch {
            settings.collect { s ->
                GeckoEngine.apply(s)
                store.updateSettings(s)
            }
        }
    }

    private fun currentProcess(): String =
        if (android.os.Build.VERSION.SDK_INT >= 28) getProcessName()
        else runCatching { java.io.File("/proc/self/cmdline").readText().trimEnd('\u0000') }.getOrDefault(packageName)
}
