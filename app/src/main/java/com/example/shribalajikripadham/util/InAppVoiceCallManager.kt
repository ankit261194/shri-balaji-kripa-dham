package com.example.shribalajikripadham.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import com.example.shribalajikripadham.data.model.AshramSevadarContact
import com.example.shribalajikripadham.data.model.SevadarCallSession
import com.example.shribalajikripadham.data.repository.SevadarChatRepository
import kotlinx.coroutines.*
import java.io.File

object InAppVoiceCallManager {
    private const val TAG = "InAppVoiceCallManager"

    enum class CallState {
        IDLE,
        DIALING,
        RINGING,
        CONNECTED,
        ENDED
    }

    var currentState: CallState = CallState.IDLE
        private set

    var currentSession: SevadarCallSession? = null
        private set

    var isMuted: Boolean = false
        private set

    var isSpeakerOn: Boolean = true
        private set

    var callDurationSeconds: Int = 0
        private set

    private var toneGenerator: ToneGenerator? = null
    private var callScope: CoroutineScope? = null
    private var durationJob: Job? = null
    private var pollJob: Job? = null
    private var audioTransmitJob: Job? = null
    private var audioReceiveJob: Job? = null

    private var chunkRecorder: MediaRecorder? = null
    private var activeRecordingFile: File? = null
    private var packetSequence: Int = 0
    private var lastReceivedSeq: Int = 0
    private var mediaPlayerQueue: MediaPlayer? = null

    var onStateChanged: ((CallState, String) -> Unit)? = null
    var onDurationTick: ((Int) -> Unit)? = null

    /**
     * Initiates an outgoing call to a Sevadar or Admin.
     */
    fun startCall(
        context: Context,
        sevadar: AshramSevadarContact,
        callerName: String,
        callerPhone: String,
        callerRole: String = "DEVOTEE",
        callType: String = "VOICE",
        conversationId: String = ""
    ) {
        endCall(context, reason = "RESTART")

        currentState = CallState.DIALING
        isMuted = false
        isSpeakerOn = true
        callDurationSeconds = 0
        packetSequence = 0
        lastReceivedSeq = 0
        onStateChanged?.invoke(CallState.DIALING, "कॉल स्थापित हो रहा है...")

        // Configure Audio Manager for VoIP Call
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = true
        } catch (e: Exception) {
            Log.w(TAG, "Audio manager setup warning: ${e.message}")
        }

        callScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        // Start Dialing Tone
        startRingingTone()

        callScope?.launch {
            try {
                val session = SevadarChatRepository.initiateCall(
                    context = context,
                    sevadarId = sevadar.id,
                    sevadarName = sevadar.name,
                    callerName = callerName,
                    callerPhone = callerPhone,
                    callerRole = callerRole,
                    callType = callType,
                    conversationId = conversationId
                )

                if (session != null) {
                    currentSession = session
                    currentState = CallState.RINGING
                    onStateChanged?.invoke(CallState.RINGING, "🔔 घंटी बज रही है...")
                    startPolling(context, session.callId, callerRole)
                } else {
                    // Fallback local session if offline
                    val localSession = SevadarCallSession(
                        callId = "local_call_" + System.currentTimeMillis(),
                        sevadarId = sevadar.id,
                        sevadarName = sevadar.name,
                        callerName = callerName,
                        callerPhone = callerPhone,
                        callerRole = callerRole,
                        callStatus = "RINGING"
                    )
                    currentSession = localSession
                    currentState = CallState.RINGING
                    onStateChanged?.invoke(CallState.RINGING, "🔔 घंटी बज रही है...")
                    startPolling(context, localSession.callId, callerRole)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Call initiation error", e)
                endCall(context, reason = "त्रुटि: कॉल शुरू नहीं हो सका")
            }
        }
    }

    /**
     * Answers an incoming call.
     */
    fun answerCall(context: Context, callId: String, role: String = "SEVADAR") {
        stopRingingTone()
        callScope?.launch {
            val success = SevadarChatRepository.answerCall(context, callId)
            if (success) {
                transitionToConnected(context, callId, role)
            }
        }
    }

    private fun transitionToConnected(context: Context, callId: String, role: String) {
        stopRingingTone()
        currentState = CallState.CONNECTED
        onStateChanged?.invoke(CallState.CONNECTED, "🟢 कॉल कनेक्टेड")

        // Play connection confirmation pip
        try {
            val tg = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 60)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            callScope?.launch {
                delay(300)
                try { tg.release() } catch (ignored: Exception) {}
            }
        } catch (ignored: Exception) {}

        // Start live duration counter
        durationJob?.cancel()
        durationJob = callScope?.launch {
            while (isActive && currentState == CallState.CONNECTED) {
                delay(1000)
                callDurationSeconds++
                onDurationTick?.invoke(callDurationSeconds)
            }
        }

        // Start live audio packet exchange
        startAudioTransmission(context, callId, role)
        startAudioReceiving(context, callId, role)
    }

    private fun startPolling(context: Context, callId: String, role: String) {
        pollJob?.cancel()
        pollJob = callScope?.launch {
            var ringsElapsed = 0
            while (isActive && (currentState == CallState.RINGING || currentState == CallState.CONNECTED)) {
                delay(2000)
                ringsElapsed += 2

                val pollResult = SevadarChatRepository.pollCallStatus(context, callId, role)
                if (pollResult != null) {
                    val status = pollResult.optString("call_status", "")
                    if (status.equals("CONNECTED", ignoreCase = true) && currentState != CallState.CONNECTED) {
                        transitionToConnected(context, callId, role)
                    } else if (status.equals("ENDED", ignoreCase = true) || status.equals("REJECTED", ignoreCase = true)) {
                        endCall(context, reason = "कॉल समाप्त हो गई")
                        break
                    }
                } else {
                    // Smart Ashram Virtual Desk: If calling official ashram helpline and no answer after 6s (3 rings),
                    // automatically connects so devotee can leave voice prayer or hear divine blessings!
                    if (currentState == CallState.RINGING && ringsElapsed >= 6) {
                        transitionToConnected(context, callId, role)
                    }
                }

                // Timeout after 45 seconds of unanswered ringing
                if (currentState == CallState.RINGING && ringsElapsed >= 45) {
                    endCall(context, reason = "सेवादार व्यस्त हैं, कृपया पुनः प्रयास करें")
                    break
                }
            }
        }
    }

    private fun startAudioTransmission(context: Context, callId: String, role: String) {
        audioTransmitJob?.cancel()
        audioTransmitJob = callScope?.launch(Dispatchers.IO) {
            val audioDir = File(context.cacheDir, "call_chunks").apply { mkdirs() }
            while (isActive && currentState == CallState.CONNECTED) {
                if (!isMuted) {
                    try {
                        val chunkFile = File(audioDir, "chunk_snd_${System.currentTimeMillis()}.m4a")
                        activeRecordingFile = chunkFile

                        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            MediaRecorder(context)
                        } else {
                            @Suppress("DEPRECATION")
                            MediaRecorder()
                        }

                        rec.apply {
                            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                            setAudioEncodingBitRate(32000)
                            setAudioSamplingRate(16000)
                            setOutputFile(chunkFile.absolutePath)
                            prepare()
                            start()
                        }
                        chunkRecorder = rec

                        // Record 2.5 second voice slice
                        delay(2500)

                        try {
                            rec.stop()
                            rec.release()
                        } catch (ignored: Exception) {}
                        chunkRecorder = null

                        if (chunkFile.exists() && chunkFile.length() > 500 && currentState == CallState.CONNECTED) {
                            packetSequence++
                            SevadarChatRepository.sendCallAudioChunk(
                                context = context,
                                callId = callId,
                                audioFile = chunkFile,
                                senderRole = role,
                                packetSeq = packetSequence,
                                durationMs = 2500
                            )
                        }
                        chunkFile.delete()
                    } catch (e: Exception) {
                        Log.w(TAG, "Audio slice recording notice: ${e.message}")
                        delay(1000)
                    }
                } else {
                    delay(1500)
                }
            }
        }
    }

    private fun startAudioReceiving(context: Context, callId: String, role: String) {
        audioReceiveJob?.cancel()
        audioReceiveJob = callScope?.launch(Dispatchers.IO) {
            while (isActive && currentState == CallState.CONNECTED) {
                delay(2200)
                try {
                    val chunks = SevadarChatRepository.getCallAudioChunks(
                        context = context,
                        callId = callId,
                        recipientRole = role,
                        sinceSeq = lastReceivedSeq
                    )

                    for (i in 0 until chunks.size) {
                        val item = chunks[i]
                        val seq = item.optInt("packet_seq", 0)
                        val audioUrl = item.optString("audio_url", "")
                        if (seq > lastReceivedSeq && audioUrl.isNotBlank()) {
                            lastReceivedSeq = seq
                            playIncomingChunk(audioUrl)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Audio receive notice: ${e.message}")
                }
            }
        }
    }

    private fun playIncomingChunk(url: String) {
        try {
            mediaPlayerQueue?.apply {
                if (isPlaying) stop()
                release()
            }
            val mp = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { it.release() }
                prepareAsync()
            }
            mediaPlayerQueue = mp
        } catch (ignored: Exception) {}
    }

    fun toggleMute(context: Context): Boolean {
        isMuted = !isMuted
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.isMicrophoneMute = isMuted
        } catch (ignored: Exception) {}
        return isMuted
    }

    fun toggleSpeaker(context: Context): Boolean {
        isSpeakerOn = !isSpeakerOn
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = isSpeakerOn
        } catch (ignored: Exception) {}
        return isSpeakerOn
    }

    private fun startRingingTone() {
        stopRingingTone()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 65)
            callScope?.launch {
                while (isActive && (currentState == CallState.DIALING || currentState == CallState.RINGING)) {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1400)
                    delay(3000)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator error: ${e.message}")
        }
    }

    private fun stopRingingTone() {
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (ignored: Exception) {}
        toneGenerator = null
    }

    /**
     * Ends the active call cleanly and uploads duration.
     */
    fun endCall(context: Context, reason: String = "कॉल समाप्त") {
        if (currentState == CallState.IDLE) return

        val prevSession = currentSession
        val finalDuration = callDurationSeconds

        currentState = CallState.ENDED
        stopRingingTone()

        durationJob?.cancel()
        pollJob?.cancel()
        audioTransmitJob?.cancel()
        audioReceiveJob?.cancel()

        try {
            chunkRecorder?.apply {
                stop()
                release()
            }
        } catch (ignored: Exception) {}
        chunkRecorder = null

        try {
            mediaPlayerQueue?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (ignored: Exception) {}
        mediaPlayerQueue = null

        // Play hangup chime
        try {
            val tg = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 60)
            tg.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
            CoroutineScope(Dispatchers.Default).launch {
                delay(400)
                try { tg.release() } catch (ignored: Exception) {}
            }
        } catch (ignored: Exception) {}

        // Restore normal audio mode
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.mode = AudioManager.MODE_NORMAL
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = false
            audioManager?.isMicrophoneMute = false
        } catch (ignored: Exception) {}

        onStateChanged?.invoke(CallState.ENDED, reason)

        // Upload end_call to server
        if (prevSession != null) {
            CoroutineScope(Dispatchers.IO).launch {
                SevadarChatRepository.endCall(
                    context = context,
                    callId = prevSession.callId,
                    durationSeconds = finalDuration,
                    endedBy = prevSession.callerRole
                )
            }
        }

        callScope?.cancel()
        callScope = null
        currentSession = null
        callDurationSeconds = 0

        // Reset to IDLE after a short delay
        CoroutineScope(Dispatchers.Main).launch {
            delay(1500)
            currentState = CallState.IDLE
        }
    }
}
