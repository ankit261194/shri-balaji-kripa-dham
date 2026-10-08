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
import com.example.shribalajikripadham.data.sacred.SACRED_TRACKS
import com.example.shribalajikripadham.data.sacred.SacredTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Playback Mode for Sacred Devotional Tracks:
 * - CONTINUOUS: Seamlessly plays next chapter/stotra/bhajan in sequence (Default)
 * - REPEAT_ONE: Jaap Loop Mode - repeats the same holy path or mantra continuously
 * - NO_REPEAT: Plays current track once and stops
 */
enum class PlaybackMode {
    CONTINUOUS,
    REPEAT_ONE,
    NO_REPEAT
}

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
        const val ACTION_NEXT = "com.example.shribalajikripadham.action.NEXT"
        const val ACTION_PREV = "com.example.shribalajikripadham.action.PREV"
        const val ACTION_CYCLE_MODE = "com.example.shribalajikripadham.action.CYCLE_MODE"

        const val EXTRA_TRACK_INDEX = "extra_track_index"
        const val EXTRA_TRACK_TITLE = "extra_track_title"
        const val EXTRA_TRACK_ARTIST = "extra_track_artist"
        const val EXTRA_TRACK_URL = "extra_track_url"
        const val EXTRA_TRACK_KEY = "extra_track_key"
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

        private val _playbackMode = MutableStateFlow(PlaybackMode.CONTINUOUS)
        val playbackMode = _playbackMode.asStateFlow()

        // Active playlist cache
        @Volatile
        private var activePlaylist: List<SacredTrack> = SACRED_TRACKS

        fun setPlaylist(tracks: List<SacredTrack>) {
            if (tracks.isNotEmpty()) {
                activePlaylist = tracks
            }
        }

        fun playTrack(
            context: Context,
            trackIndex: Int,
            title: String,
            artist: String,
            audioUrl: String,
            trackKey: String = "",
            playlist: List<SacredTrack>? = null
        ) {
            playlist?.let { if (it.isNotEmpty()) activePlaylist = it }
            val intent = Intent(context, BhajanAudioService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_TRACK_INDEX, trackIndex)
                putExtra(EXTRA_TRACK_TITLE, title)
                putExtra(EXTRA_TRACK_ARTIST, artist)
                putExtra(EXTRA_TRACK_URL, audioUrl)
                putExtra(EXTRA_TRACK_KEY, trackKey)
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

        fun playNext(context: Context) {
            val intent = Intent(context, BhajanAudioService::class.java).apply {
                action = ACTION_NEXT
            }
            context.startService(intent)
        }

        fun playPrevious(context: Context) {
            val intent = Intent(context, BhajanAudioService::class.java).apply {
                action = ACTION_PREV
            }
            context.startService(intent)
        }

        fun cyclePlaybackMode(context: Context) {
            val nextMode = when (_playbackMode.value) {
                PlaybackMode.CONTINUOUS -> PlaybackMode.REPEAT_ONE
                PlaybackMode.REPEAT_ONE -> PlaybackMode.NO_REPEAT
                PlaybackMode.NO_REPEAT -> PlaybackMode.CONTINUOUS
            }
            _playbackMode.value = nextMode
            val intent = Intent(context, BhajanAudioService::class.java).apply {
                action = ACTION_CYCLE_MODE
            }
            context.startService(intent)
        }

        fun setPlaybackMode(context: Context, mode: PlaybackMode) {
            _playbackMode.value = mode
            val intent = Intent(context, BhajanAudioService::class.java).apply {
                action = ACTION_CYCLE_MODE
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

        fun seekForward(context: Context, deltaMs: Int = 10000) {
            val newPos = (_currentPositionMs.value + deltaMs).coerceAtMost(_durationMs.value)
            seekTo(context, newPos)
        }

        fun seekBackward(context: Context, deltaMs: Int = 10000) {
            val newPos = (_currentPositionMs.value - deltaMs).coerceAtLeast(0)
            seekTo(context, newPos)
        }

        private val _sleepTimerMinutes = MutableStateFlow(0)
        val sleepTimerMinutes = _sleepTimerMinutes.asStateFlow()
        private var sleepTimerJob: Job? = null

        fun setSleepTimer(context: Context, minutes: Int) {
            _sleepTimerMinutes.value = minutes
            sleepTimerJob?.cancel()
            if (minutes > 0) {
                sleepTimerJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(minutes * 60 * 1000L)
                    _sleepTimerMinutes.value = 0
                    stopPlayback(context)
                }
            }
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private var trackTitle: String = ""
    private var trackArtist: String = ""
    private var trackUrl: String = ""
    private var trackIndex: Int = -1

    // AudioFocus Management: Prevents playing over phone calls or other media apps
    private var audioManager: android.media.AudioManager? = null
    private var focusRequestObj: Any? = null
    private var pausedByTransientLoss = false

    private val audioFocusChangeListener = android.media.AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            android.media.AudioManager.AUDIOFOCUS_LOSS -> {
                pausedByTransientLoss = false
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    _isPlaying.value = false
                    updateNotification(false)
                }
            }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Incoming phone call or navigation prompt: automatically pause
                if (mediaPlayer?.isPlaying == true) {
                    pausedByTransientLoss = true
                    mediaPlayer?.pause()
                    _isPlaying.value = false
                    updateNotification(false)
                }
            }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Notification beep: duck audio volume temporarily
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.setVolume(0.2f, 0.2f)
                    }
                } catch (e: Exception) {}
            }
            android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                try {
                    mediaPlayer?.setVolume(1.0f, 1.0f)
                } catch (e: Exception) {}
                if (pausedByTransientLoss) {
                    pausedByTransientLoss = false
                    if (mediaPlayer != null) {
                        mediaPlayer?.start()
                        _isPlaying.value = true
                        updateNotification(true)
                    }
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            val req = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            focusRequestObj = req
            return audioManager?.requestAudioFocus(req) == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            return audioManager?.requestAudioFocus(
                audioFocusChangeListener,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN
            ) == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        try {
            audioManager?.let { am ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val req = focusRequestObj as? android.media.AudioFocusRequest
                    if (req != null) am.abandonAudioFocusRequest(req)
                } else {
                    @Suppress("DEPRECATION")
                    am.abandonAudioFocus(audioFocusChangeListener)
                }
            }
        } catch (e: Exception) {}
    }

    private val noisyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    _isPlaying.value = false
                    updateNotification(false)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            val filter = android.content.IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            registerReceiver(noisyReceiver, filter)
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_PLAY -> {
                val index = intent.getIntExtra(EXTRA_TRACK_INDEX, -1)
                val title = intent.getStringExtra(EXTRA_TRACK_TITLE) ?: "श्री बालाजी भजन"
                val artist = intent.getStringExtra(EXTRA_TRACK_ARTIST) ?: "श्री बालाजी कृपा धाम"
                val url = intent.getStringExtra(EXTRA_TRACK_URL) ?: ""
                val trackKey = intent.getStringExtra(EXTRA_TRACK_KEY) ?: ""

                trackIndex = index
                trackTitle = title
                trackArtist = artist
                trackUrl = url

                _currentTrackIndex.value = index
                _currentTitle.value = title
                _currentArtist.value = artist

                startForeground(NOTIFICATION_ID, buildNotification(isPlaying = true))
                startAudioPlayback(url, trackKey)
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
            ACTION_NEXT -> {
                playNextTrackInternal()
            }
            ACTION_PREV -> {
                playPreviousTrackInternal()
            }
            ACTION_CYCLE_MODE -> {
                updateNotification(mediaPlayer?.isPlaying == true)
            }
            ACTION_STOP -> {
                stopSelfService()
            }
        }

        return START_NOT_STICKY
    }

    private fun playNextTrackInternal() {
        val list = activePlaylist.ifEmpty { SACRED_TRACKS }
        if (list.isEmpty()) return
        val nextIndex = if (trackIndex in list.indices) (trackIndex + 1) % list.size else 0
        playTrackByIndex(nextIndex, list)
    }

    private fun playPreviousTrackInternal() {
        // Rewind to 0 if already played more than 3 seconds
        if ((mediaPlayer?.currentPosition ?: 0) > 3000) {
            mediaPlayer?.seekTo(0)
            _currentPositionMs.value = 0
            return
        }
        val list = activePlaylist.ifEmpty { SACRED_TRACKS }
        if (list.isEmpty()) return
        val prevIndex = if (trackIndex in list.indices) {
            if (trackIndex - 1 < 0) list.size - 1 else trackIndex - 1
        } else {
            0
        }
        playTrackByIndex(prevIndex, list)
    }

    private fun playTrackByIndex(index: Int, list: List<SacredTrack>) {
        if (index !in list.indices) return
        val track = list[index]
        trackIndex = index
        trackTitle = track.titleHindi
        trackArtist = track.subtitleHindi.ifBlank { "श्री बालाजी कृपा धाम" }
        trackUrl = track.audioUrl

        _currentTrackIndex.value = index
        _currentTitle.value = track.titleHindi
        _currentArtist.value = track.subtitleHindi

        startForeground(NOTIFICATION_ID, buildNotification(isPlaying = true))
        startAudioPlayback(track.audioUrl, track.trackKey)
    }

    private fun startAudioPlayback(url: String, trackKey: String) {
        if (url.isBlank()) return

        val playableSource = com.example.shribalajikripadham.util.DevotionalAudioCacheManager.getPlayableSource(
            applicationContext, trackKey, url
        )
        val isLocalOfflineFile = java.io.File(playableSource).exists()

        _isBuffering.value = !isLocalOfflineFile
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
                setDataSource(playableSource)
                setOnPreparedListener(this@BhajanAudioService)
                setOnCompletionListener(this@BhajanAudioService)
                setOnErrorListener(this@BhajanAudioService)

                if (isLocalOfflineFile) {
                    // Synchronous instant 0.0s preparation for offline files
                    prepare()
                    requestAudioFocus()
                    start()
                    _isBuffering.value = false
                    _isPlaying.value = true
                    _durationMs.value = duration
                    updateNotification(true)
                    startProgressTracker()
                } else {
                    prepareAsync()
                    // Silently download in background so next time is instant 0ms offline
                    serviceScope.launch(Dispatchers.IO) {
                        try {
                            com.example.shribalajikripadham.util.DevotionalAudioCacheManager.downloadTrackForOffline(
                                applicationContext, trackKey, url
                            )
                        } catch (ignored: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback: ${e.message}")
            _isBuffering.value = false
            _isPlaying.value = false
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        _isBuffering.value = false
        requestAudioFocus()
        mp?.start()
        _isPlaying.value = true
        _durationMs.value = mp?.duration ?: 0

        updateNotification(true)
        startProgressTracker()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        when (_playbackMode.value) {
            PlaybackMode.REPEAT_ONE -> {
                // Jaap Loop Mode: seamlessly replay the same sacred path / stotra
                try {
                    _currentPositionMs.value = 0
                    mp?.seekTo(0)
                    mp?.start()
                    _isPlaying.value = true
                    updateNotification(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in repeat loop: ${e.message}")
                    _isPlaying.value = false
                    updateNotification(false)
                }
            }
            PlaybackMode.CONTINUOUS -> {
                // Continuous Auto-Advance: seamlessly plays next chapter/stotra
                _currentPositionMs.value = 0
                playNextTrackInternal()
            }
            PlaybackMode.NO_REPEAT -> {
                _isPlaying.value = false
                _currentPositionMs.value = 0
                updateNotification(false)
            }
        }
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

        val prevIntent = Intent(this, BhajanAudioService::class.java).apply { action = ACTION_PREV }
        val prevPending = PendingIntent.getService(
            this, 104, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, BhajanAudioService::class.java).apply { action = ACTION_TOGGLE }
        val togglePending = PendingIntent.getService(
            this, 102, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, BhajanAudioService::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getService(
            this, 105, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BhajanAudioService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 103, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val toggleText = if (isPlaying) "विराम" else "चलाएं"

        val modeLabel = when (_playbackMode.value) {
            PlaybackMode.CONTINUOUS -> "निरंतर पाठ (Auto-Advance)"
            PlaybackMode.REPEAT_ONE -> "जाप लूप (Repeat Current)"
            PlaybackMode.NO_REPEAT -> "एक बार (Single)"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(trackTitle.ifBlank { "श्री बालाजी पावन भजन" })
            .setContentText(trackArtist.ifBlank { "श्री बालाजी कृपा धाम, डूँगरा जाट" })
            .setSubText(modeLabel)
            .setContentIntent(openPending)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_previous, "पिछली", prevPending)
            .addAction(toggleIcon, toggleText, togglePending)
            .addAction(android.R.drawable.ic_media_next, "अगली", nextPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "बंद करें", stopPending)
            .build()
    }

    private fun updateNotification(isPlaying: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(isPlaying))
    }

    private fun stopSelfService() {
        abandonAudioFocus()
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
        try {
            unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}
        stopSelfService()
    }
}
