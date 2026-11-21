package com.zuno.browser.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.zuno.browser.MainActivity
import com.zuno.browser.R

/**
 * Foreground service that keeps Gecko's audio alive in the background and exposes it to Android:
 * media notification, lock-screen / quick-settings media controls, headset buttons.
 */
class MediaPlaybackService : Service() {
    private lateinit var session: MediaSessionCompat
    private var foreground = false

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Media playback", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Controls for music and videos playing in Zuno"
                    setShowBadge(false)
                },
            )
        }
        session = MediaSessionCompat(this, "ZunoMedia").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { MediaHub.play() }
                override fun onPause() { MediaHub.pause() }
                override fun onSkipToNext() { MediaHub.next() }
                override fun onSkipToPrevious() { MediaHub.previous() }
                override fun onSeekTo(pos: Long) { MediaHub.seekTo(pos) }
                override fun onStop() { MediaHub.stop() }
            })
            isActive = true
        }
        MediaHub.listener = { np -> if (np == null) shutdown() else render(np) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> MediaHub.toggle()
            ACTION_NEXT -> MediaHub.next()
            ACTION_PREV -> MediaHub.previous()
            ACTION_STOP -> { MediaHub.stop(); return START_NOT_STICKY }
        }
        val np = MediaHub.nowPlaying
        // Started via startForegroundService(): Android requires startForeground() within seconds,
        // even if the page paused in the meantime, or it kills the app. Enter the foreground first,
        // then let render() drop it again if we're paused.
        ensureForeground(np)
        if (np == null) shutdown() else render(np)
        return START_NOT_STICKY
    }

    private fun ensureForeground(np: NowPlaying?) {
        if (foreground) return
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_zuno)
            .setContentTitle(np?.title ?: "Zuno Browser")
            .setContentText(np?.artist ?: "Playing media")
            .setSilent(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        runCatching { ServiceCompat.startForeground(this, NOTIF_ID, n, type); foreground = true }
    }

    /** Swiped away from recents: stop the media and take the notification (and badge) with us. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        MediaHub.pause()
        MediaHub.clear()
        shutdown()
        super.onTaskRemoved(rootIntent)
    }

    private fun action(action: String, code: Int): PendingIntent =
        PendingIntent.getService(this, code, Intent(this, MediaPlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun render(np: NowPlaying) {
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, np.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, np.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, np.site)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, np.durationMs)
                .apply { np.artwork?.let { putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) } }
                .build(),
        )
        var actions = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SEEK_TO
        if (np.canNext) actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        if (np.canPrev) actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(if (np.playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, np.positionMs, if (np.playing) 1f else 0f)
                .build(),
        )

        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_zuno)
            .setContentTitle(np.title)
            .setContentText(np.artist)
            .setSubText(np.site)
            .setLargeIcon(np.artwork)
            .setContentIntent(open)
            .setDeleteIntent(action(ACTION_STOP, 4))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(np.playing)
            .setSilent(true)
        var compact = intArrayOf()
        var i = 0
        if (np.canPrev) { b.addAction(R.drawable.ic_media_prev, "Previous", action(ACTION_PREV, 1)); i++ }
        b.addAction(if (np.playing) R.drawable.ic_media_pause else R.drawable.ic_media_play, if (np.playing) "Pause" else "Play", action(ACTION_TOGGLE, 2))
        compact += i; i++
        if (np.canNext) { b.addAction(R.drawable.ic_media_next, "Next", action(ACTION_NEXT, 3)); compact += i }
        b.setStyle(MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(*compact))
        val notification = b.build()

        if (np.playing) {
            val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            runCatching { ServiceCompat.startForeground(this, NOTIF_ID, notification, type); foreground = true }
                .onFailure { runCatching { NotificationManagerCompat.from(this).notify(NOTIF_ID, notification) } }
        } else {
            // Paused: drop foreground (lets the system reclaim us) but keep the controls visible.
            if (foreground) { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH); foreground = false }
            runCatching { NotificationManagerCompat.from(this).notify(NOTIF_ID, notification) }
        }
    }

    private fun shutdown() {
        session.isActive = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(NOTIF_ID)
        foreground = false
        stopSelf()
    }

    override fun onDestroy() {
        MediaHub.listener = null
        NotificationManagerCompat.from(this).cancel(NOTIF_ID)
        session.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "media"
        private const val NOTIF_ID = 42
        const val ACTION_TOGGLE = "zuno.media.TOGGLE"
        const val ACTION_NEXT = "zuno.media.NEXT"
        const val ACTION_PREV = "zuno.media.PREV"
        const val ACTION_STOP = "zuno.media.STOP"
    }
}
