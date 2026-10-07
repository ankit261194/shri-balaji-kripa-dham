package com.example.shribalajikripadham.util

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

object AudioPlayerHelper {
    private const val TAG = "AudioPlayerHelper"
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    var currentPlayingUrl: String? = null
        private set

    fun isPlaying(url: String?): Boolean {
        return url != null && url == currentPlayingUrl && (mediaPlayer?.isPlaying == true)
    }

    fun play(
        urlOrPath: String,
        onProgress: (currentMs: Int, totalMs: Int) -> Unit = { _, _ -> },
        onComplete: () -> Unit = {}
    ) {
        try {
            if (currentPlayingUrl == urlOrPath && mediaPlayer != null) {
                if (mediaPlayer?.isPlaying == true) {
                    pause()
                    return
                } else {
                    mediaPlayer?.start()
                    startProgressTracker(onProgress)
                    return
                }
            }

            stop()

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(urlOrPath)
                setOnPreparedListener { player ->
                    player.start()
                    currentPlayingUrl = urlOrPath
                    startProgressTracker(onProgress)
                }
                setOnCompletionListener {
                    stopProgressTracker()
                    currentPlayingUrl = null
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    stopProgressTracker()
                    currentPlayingUrl = null
                    onComplete()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            stop()
            onComplete()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            stopProgressTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio", e)
        }
    }

    fun stop() {
        try {
            stopProgressTracker()
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (ignored: Exception) {
        } finally {
            mediaPlayer = null
            currentPlayingUrl = null
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio", e)
        }
    }

    private fun startProgressTracker(onProgress: (Int, Int) -> Unit) {
        stopProgressTracker()
        progressRunnable = object : Runnable {
            override fun run() {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    val current = mp.currentPosition
                    val total = mp.duration
                    onProgress(current, total)
                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressTracker() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }
}
