package com.zuno.browser.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.MediaSession

/** What's playing right now, for the notification and the in-app UI. */
data class NowPlaying(
    val title: String,
    val artist: String,
    val site: String,
    val artwork: Bitmap?,
    val playing: Boolean,
    val canPrev: Boolean,
    val canNext: Boolean,
    val durationMs: Long,
    val positionMs: Long,
)

/**
 * Bridges Gecko's W3C MediaSession (what YouTube / YouTube Music / Spotify web expose) to Android:
 * tracks the active page's media, starts [MediaPlaybackService] while something plays so audio
 * survives the app going to the background, and routes notification buttons back to the page.
 */
object MediaHub {
    private lateinit var app: Context
    private val main = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    var session: GeckoSession? = null
        private set
    private var media: MediaSession? = null
    private var features: Long = 0
    private var meta: MediaSession.Metadata? = null
    private var artwork: Bitmap? = null
    private var site: String = ""
    private var duration = 0.0
    private var position = 0.0

    var nowPlaying by mutableStateOf<NowPlaying?>(null)
        private set

    /** Notified on every change so the service can refresh its notification. */
    var listener: ((NowPlaying?) -> Unit)? = null

    fun init(context: Context) { app = context.applicationContext }

    fun isPlaying(s: GeckoSession?): Boolean = s != null && s === session && nowPlaying?.playing == true

    fun delegate(siteOf: () -> String): MediaSession.Delegate = object : MediaSession.Delegate {
        override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
            this@MediaHub.session = session
            media = mediaSession
            site = siteOf()
            publish()
        }

        override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {
            if (this@MediaHub.session === session) clear()
        }

        override fun onMetadata(session: GeckoSession, mediaSession: MediaSession, meta: MediaSession.Metadata) {
            if (this@MediaHub.session !== session) return
            this@MediaHub.meta = meta
            artwork = null
            site = siteOf()
            publish()
            meta.artwork?.getBitmap(256)?.accept({ bmp ->
                if (this@MediaHub.meta === meta) { artwork = bmp; publish() }
            }, { })
        }

        override fun onFeatures(session: GeckoSession, mediaSession: MediaSession, features: Long) {
            if (this@MediaHub.session === session) { this@MediaHub.features = features; publish() }
        }

        override fun onPlay(session: GeckoSession, mediaSession: MediaSession) {
            this@MediaHub.session = session
            media = mediaSession
            site = siteOf()
            publish(playing = true)
        }

        override fun onPause(session: GeckoSession, mediaSession: MediaSession) {
            if (this@MediaHub.session === session) publish(playing = false)
        }

        override fun onStop(session: GeckoSession, mediaSession: MediaSession) {
            if (this@MediaHub.session === session) publish(playing = false)
        }

        override fun onPositionState(session: GeckoSession, mediaSession: MediaSession, state: MediaSession.PositionState) {
            if (this@MediaHub.session !== session) return
            duration = state.duration
            position = state.position
            publish()
        }
    }

    private var lastPlaying = false

    private fun publish(playing: Boolean = lastPlaying) {
        lastPlaying = playing
        val m = meta
        val np = NowPlaying(
            title = m?.title?.takeIf { it.isNotBlank() } ?: site.ifBlank { "Playing" },
            artist = m?.artist?.takeIf { it.isNotBlank() } ?: site,
            site = site,
            artwork = artwork,
            playing = playing,
            canPrev = features and MediaSession.Feature.PREVIOUS_TRACK != 0L,
            canNext = features and MediaSession.Feature.NEXT_TRACK != 0L,
            durationMs = (duration * 1000).toLong(),
            positionMs = (position * 1000).toLong(),
        )
        nowPlaying = np
        listener?.invoke(np)
        if (playing) {
            stopRunnable?.let { main.removeCallbacks(it) }
            stopRunnable = null
            startService()
        } else if (stopRunnable == null) {
            // Keep the notification around while paused so the user can resume; drop it after a while.
            stopRunnable = Runnable { clear() }.also { main.postDelayed(it, 10 * 60_000L) }
        }
    }

    /** True while the media notification couldn't be shown because notifications are off. */
    var needsNotificationPermission by mutableStateOf(false)
        private set

    private fun startService() {
        if (!::app.isInitialized) return
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(app, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) needsNotificationPermission = true
        runCatching { ContextCompat.startForegroundService(app, Intent(app, MediaPlaybackService::class.java)) }
            .onFailure { android.util.Log.w("ZunoMedia", "could not start media service", it) }
    }

    fun permissionHandled() { needsNotificationPermission = false }

    /** Forget the media session (tab closed, media gone) and tear down the notification. */
    fun clear() {
        stopRunnable?.let { main.removeCallbacks(it) }
        stopRunnable = null
        session = null; media = null; meta = null; artwork = null; features = 0; lastPlaying = false
        nowPlaying = null
        listener?.invoke(null)
    }

    fun forget(s: GeckoSession) { if (s === session) clear() }

    fun play() = media?.play()
    fun pause() = media?.pause()
    fun toggle() { if (nowPlaying?.playing == true) pause() else play() }
    fun next() = media?.nextTrack()
    fun previous() = media?.previousTrack()
    fun seekTo(ms: Long) = media?.seekTo(ms / 1000.0, false)
    fun stop() { media?.pause(); clear() }
}
