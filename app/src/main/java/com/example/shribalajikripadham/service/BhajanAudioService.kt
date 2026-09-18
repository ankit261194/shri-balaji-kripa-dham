package com.example.shribalajikripadham.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.shribalajikripadham.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BhajanAudioService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    companion object {
        private const val TAG = "BhajanAudioService"
        const val NOTIFICATION_ID = 54321
        const val CHANNEL_ID = "bhajan_playback_channel"

        const val ACTION_PLAY = "com.example.shribalajikripadham.action.PLAY"
        const val ACTION_PAUSE = "com.example.shribalajikripadham.action.PAUSE"
        const val ACTION_TOGGLE = "com.example.shribalajikripadham.action.TOGGLE"
        const val ACTION_STOP = "com.example.shribalajikripadham.action.STOP"
        const val ACTION_SEEK = "com.example.shribalajikripadham.action.SEEK"

        const val EXTRA_TRACK_INDEX = "extra_track_index"
        const val EXTRA_TRACK_TITLE = "extra_track_title"
        const val EXTRA_TRACK_ARTIST = "extra_track_artist"
        const val EXTRA_TRACK_URL = "extra_track_url"
        const val EXTRA_SEEK_POS = "extra_seek_pos"

        private val _currentTrackIndex = MutableStateFlow(-1)
        val currentTrackIndex = _currentTrackIndex.asStateFlow()

        private val _isPlaying = MutableStateFlow(false)
        val isPlaying = _isPlaying.asStateFlow()

        private val _isBuffering = MutableStateFlow(false)
        val isBuffering = _isBuffering.asStateFlow()

        private val _currentPositionMs = MutableStateFlow(0)
        val currentPositionMs = _currentPositionMs.asStateFlow()

        private val _durationMs = MutableStateFlow(0)
        val durationMs = _durationMs.asStateFlow()

        private val _currentTitle = MutableStateFlow("")
        val currentTitle = _currentTitle.asStateFlow()

        private val _currentArtist = MutableStateFlow("")
        val currentArtist = _currentArtist.asStateFlow()

        fun playTrack(context: Context, trackIndex: Int, title: String, artist: String, audioUrl: String) {
            val intent = Intent(context, BhajanAudioService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_TRACK_INDEX, trackIndex)
                putExtra(EXTRA_TRACK_TITLE, title)
                putExtra(EXTRA_TRACK_ARTIST, artist)
                putExtra(EXTRA_TRACK_URL, audioUrl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun togglePlayPause(context: Context) {
            val intent = Intent(context, BhajanAudioService::class.java).apply {
                action = ACTION_TOGGLE
            }
            context.startService(intent)
        }

        fun stopPlayback(context: Context) {
            val intent = Intent(context, BhajanAudioService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun seekTo(context: Context, positionMs: Int) {
            val intent = Intent(context, BhajanAudioService::class.java).apply {
                action = ACTION_SEEK
                putExtra(EXTRA_SEEK_POS, positionMs)
            }
            context.startService(intent)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private var trackTitle: String = ""
    private var trackArtist: String = ""
    private var trackUrl: String = ""
    private var trackIndex: Int = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_PLAY -> {
                val index = intent.getIntExtra(EXTRA_TRACK_INDEX, -1)
                val title = intent.getStringExtra(EXTRA_TRACK_TITLE) ?: "श्री बालाजी भजन"
                val artist = intent.getStringExtra(EXTRA_TRACK_ARTIST) ?: "श्री बालाजी कृपा धाम"
                val url = intent.getStringExtra(EXTRA_TRACK_URL) ?: ""

                trackIndex = index
                trackTitle = title
                trackArtist = artist
                trackUrl = url

                _currentTrackIndex.value = index
                _currentTitle.value = title
                _currentArtist.value = artist

                startForeground(NOTIFICATION_ID, buildNotification(isPlaying = true))
                startAudioPlayback(url)
            }
            ACTION_TOGGLE -> {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    _isPlaying.value = false
                    updateNotification(false)
                } else if (mediaPlayer != null) {
                    mediaPlayer?.start()
                    _isPlaying.value = true
                    updateNotification(true)
                }
            }
            ACTION_PAUSE -> {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    _isPlaying.value = false
                    updateNotification(false)
                }
            }
            ACTION_SEEK -> {
                val pos = intent.getIntExtra(EXTRA_SEEK_POS, 0)
                mediaPlayer?.seekTo(pos)
                _currentPositionMs.value = pos
            }
            ACTION_STOP -> {
                stopSelfService()
            }
        }

        return START_NOT_STICKY
    }

    private fun startAudioPlayback(url: String) {
        if (url.isBlank()) return

        _isBuffering.value = true
        _isPlaying.value = false

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener(this@BhajanAudioService)
                setOnCompletionListener(this@BhajanAudioService)
                setOnErrorListener(this@BhajanAudioService)
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback: ${e.message}")
            _isBuffering.value = false
            _isPlaying.value = false
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        _isBuffering.value = false
        mp?.start()
        _isPlaying.value = true
        _durationMs.value = mp?.duration ?: 0

        updateNotification(true)
        startProgressTracker()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        _isPlaying.value = false
        _currentPositionMs.value = 0
        updateNotification(false)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
        _isBuffering.value = false
        _isPlaying.value = false
        updateNotification(false)
        return true
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        _currentPositionMs.value = mediaPlayer?.currentPosition ?: 0
                        _durationMs.value = mediaPlayer?.duration ?: 0
                    }
                } catch (e: Exception) {}
                delay(1000L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "श्री बालाजी पावन भजन व आरती (Bhajan Playback)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "पृष्ठभूमि में भजन व आरती का निरंतर पावन प्रवाह"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_LIVE_DARBAR", true)
        }
        val openPending = PendingIntent.getActivity(
            this, 101, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, BhajanAudioService::class.java).apply { action = ACTION_TOGGLE }
        val togglePending = PendingIntent.getService(
            this, 102, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BhajanAudioService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 103, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val toggleText = if (isPlaying) "विराम (Pause)" else "चलाएं (Play)"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(trackTitle.ifBlank { "श्री बालाजी पावन भजन" })
            .setContentText(trackArtist.ifBlank { "श्री बालाजी कृपा धाम, डूँगरा जाट" })
            .setSubText("॥ श्री हनुमते नमः ॥")
            .setContentIntent(openPending)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(toggleIcon, toggleText, togglePending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "बंद करें (Stop)", stopPending)
            .build()
    }

    private fun updateNotification(isPlaying: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(isPlaying))
    }

    private fun stopSelfService() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {}

        _isPlaying.value = false
        _isBuffering.value = false
        _currentTrackIndex.value = -1
        _currentPositionMs.value = 0

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelfService()
    }
}
