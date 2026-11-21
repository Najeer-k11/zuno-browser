package com.zuno.browser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.zuno.browser.ui.browser.ZunoRoot
import com.zuno.browser.ui.theme.DarkMode
import com.zuno.browser.ui.theme.ExpressiveTheme
import com.zuno.browser.ui.theme.Palette
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app get() = application as ZunoApp

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val settings by app.settings.collectAsStateWithLifecycle()
            // Private mode recolours the whole app (dark violet) so it's unmistakable.
            val theme = if (app.store.privateMode) {
                settings.theme.copy(darkMode = DarkMode.DARK, dynamicColor = false, palette = Palette.VIOLET)
            } else settings.theme
            ExpressiveTheme(theme) {
                ZunoRoot(
                    store = app.store,
                    settings = settings,
                    onUpdate = { f -> lifecycleScope.launch { app.repo.update(f) } },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        app.store.saveNow()
    }

    /** VIEW (links from other apps), SEND (shared text) and WEB_SEARCH open in a new tab. */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val store = app.store
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString?.let { store.newTab(it, external = true) }
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                val url = Regex("https?://\\S+").find(text)?.value
                store.submit(url ?: text, newTab = true)
            }
            Intent.ACTION_WEB_SEARCH -> intent.getStringExtra("query")?.let { store.submit(it, newTab = true) }
        }
    }
}
