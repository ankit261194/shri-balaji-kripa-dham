package com.example.shribalajikripadham.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

object VoiceRecorderHelper {
    private const val TAG = "VoiceRecorderHelper"
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L
    var isRecording: Boolean = false
        private set

    @Suppress("DEPRECATION")
    fun startRecording(context: Context): Boolean {
        try {
            cancelRecording()

            val voiceDir = File(context.cacheDir, "voice_notes")
            if (!voiceDir.exists()) {
                voiceDir.mkdirs()
            }

            val file = File(voiceDir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = rec
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recording", e)
            cancelRecording()
            return false
        }
    }

    /**
     * Stops the active recording and returns the recorded File and duration in seconds.
     */
    fun stopRecording(): Pair<File, Int>? {
        if (!isRecording || recorder == null) return null
        return try {
            val durationSec = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt().coerceAtLeast(1)
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false

            val file = currentOutputFile
            currentOutputFile = null

            if (file != null && file.exists() && file.length() > 0) {
                Pair(file, durationSec)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice recording", e)
            cancelRecording()
            null
        }
    }

    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (ignored: Exception) {
        } finally {
            recorder = null
            isRecording = false
            try {
                currentOutputFile?.delete()
            } catch (ignored: Exception) {}
            currentOutputFile = null
        }
    }
}
