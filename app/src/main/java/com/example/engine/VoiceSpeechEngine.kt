package com.example.engine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class VoiceEngineState(
    val ttsReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val isListening: Boolean = false,
    val partialTranscript: String = "",
    val finalTranscript: String = "",
    val rmsDb: Float = 0f,
    val speechAvailable: Boolean = true,
    val statusMessage: String = "준비 완료",
    val errorNotice: String? = null,
    val errorTimestampMs: Long = 0L,
    val activeSpokenText: String = "",
    val spokenCharStart: Int = -1,
    val spokenCharEnd: Int = -1,
    val speechBeatIndex: Int = 0
)

private data class PendingSpeechRequest(
    val text: String,
    val locale: Locale,
    val pitch: Float,
    val rate: Float,
    val onDone: (() -> Unit)?
)

/**
 * Hardened Real-Time TTS & STT Engine ("듣고 말하기"):
 * - Active utteranceId gating eliminates stale onDone/onError races on rapid replay taps
 * - Separate TTS vs STT teardown prevents stopListening() from killing live TTS word cadence
 * - Speech AudioAttributes + 140ms speaker-drain guard prevents mic echo on speaker-to-mic transitions
 * - Preserves captured partial transcripts on pause/manual-stop so learner speech is never lost
 */
class VoiceSpeechEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(
        VoiceEngineState(
            speechAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        )
    )
    val state: StateFlow<VoiceEngineState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var listenStartTimeMs: Long = 0L
    private var lastTtsStoppedAtMs: Long = 0L
    private var readyForSpeechReceived: Boolean = false
    private var readinessWatchdog: Runnable? = null
    private var delayedListenRunnable: Runnable? = null

    @Volatile
    private var activeUtteranceId: String? = null
    private var currentUtteranceText: String = ""
    private var currentUtteranceRate: Float = 1.0f
    private var rangeStartSupportedForUtterance: Boolean = false
    private var fallbackCadenceRunnable: Runnable? = null
    private var latestCapturedPartial: String = ""

    private var pendingSpeech: PendingSpeechRequest? = null
    private var onFinalResultCallback: ((transcript: String, durationMs: Long) -> Unit)? = null
    private var onTtsDoneCallback: (() -> Unit)? = null

    init {
        initTts()
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun stopFallbackCadence() {
        fallbackCadenceRunnable?.let { mainHandler.removeCallbacks(it) }
        fallbackCadenceRunnable = null
    }

    private fun cancelDelayedListen() {
        delayedListenRunnable?.let { mainHandler.removeCallbacks(it) }
        delayedListenRunnable = null
    }

    private fun startFallbackWordCadence(utteranceId: String, text: String, rate: Float) {
        stopFallbackCadence()
        val matches = Regex("\\S+").findAll(text).toList()
        if (matches.isEmpty()) return
        var idx = 0
        val stepMs = (320f / rate.coerceIn(0.6f, 1.4f)).toLong().coerceIn(170L, 520L)
        val ticker = object : Runnable {
            override fun run() {
                if (activeUtteranceId != utteranceId || !_state.value.isSpeaking || rangeStartSupportedForUtterance) return
                if (idx < matches.size) {
                    val m = matches[idx]
                    idx++
                    _state.value = _state.value.copy(
                        spokenCharStart = m.range.first,
                        spokenCharEnd = m.range.last + 1,
                        speechBeatIndex = _state.value.speechBeatIndex + 1
                    )
                    mainHandler.postDelayed(this, stepMs)
                }
            }
        }
        fallbackCadenceRunnable = ticker
        mainHandler.postDelayed(ticker, 200L)
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    tts?.language = Locale.US
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            if (utteranceId == null || utteranceId != activeUtteranceId) return
                            rangeStartSupportedForUtterance = false
                            val textSnapshot = currentUtteranceText
                            val rateSnapshot = currentUtteranceRate
                            _state.value = _state.value.copy(
                                isSpeaking = true,
                                activeSpokenText = textSnapshot,
                                spokenCharStart = 0,
                                spokenCharEnd = 0,
                                speechBeatIndex = _state.value.speechBeatIndex + 1,
                                statusMessage = "발음 재생 중"
                            )
                            mainHandler.post { startFallbackWordCadence(utteranceId, textSnapshot, rateSnapshot) }
                        }

                        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                            if (utteranceId == null || utteranceId != activeUtteranceId) return
                            rangeStartSupportedForUtterance = true
                            mainHandler.post { stopFallbackCadence() }
                            _state.value = _state.value.copy(
                                spokenCharStart = start,
                                spokenCharEnd = end,
                                speechBeatIndex = _state.value.speechBeatIndex + 1
                            )
                        }

                        override fun onDone(utteranceId: String?) {
                            if (utteranceId == null || utteranceId != activeUtteranceId) return
                            activeUtteranceId = null
                            lastTtsStoppedAtMs = System.currentTimeMillis()
                            mainHandler.post { stopFallbackCadence() }
                            _state.value = _state.value.copy(
                                isSpeaking = false,
                                spokenCharStart = -1,
                                spokenCharEnd = -1,
                                statusMessage = "듣기 완료"
                            )
                            val cb = onTtsDoneCallback
                            onTtsDoneCallback = null
                            if (cb != null) {
                                mainHandler.post { cb() }
                            }
                        }

                        override fun onStop(utteranceId: String?, interrupted: Boolean) {
                            if (utteranceId != null && utteranceId != activeUtteranceId) return
                            activeUtteranceId = null
                            lastTtsStoppedAtMs = System.currentTimeMillis()
                            mainHandler.post { stopFallbackCadence() }
                            _state.value = _state.value.copy(
                                isSpeaking = false,
                                spokenCharStart = -1,
                                spokenCharEnd = -1
                            )
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            if (utteranceId != null && utteranceId != activeUtteranceId) return
                            activeUtteranceId = null
                            lastTtsStoppedAtMs = System.currentTimeMillis()
                            mainHandler.post { stopFallbackCadence() }
                            _state.value = _state.value.copy(
                                isSpeaking = false,
                                spokenCharStart = -1,
                                spokenCharEnd = -1,
                                statusMessage = "음성 출력 오류"
                            )
                            onTtsDoneCallback = null
                        }
                    })
                    _state.value = _state.value.copy(ttsReady = true, statusMessage = "준비 완료")
                    val queued = pendingSpeech
                    pendingSpeech = null
                    if (queued != null) {
                        mainHandler.post {
                            speak(queued.text, queued.locale, queued.pitch, queued.rate, queued.onDone)
                        }
                    }
                } else {
                    Log.w("VoiceSpeechEngine", "TTS init failed with status=$status")
                    _state.value = _state.value.copy(ttsReady = false, statusMessage = "TTS 대기")
                    pendingSpeech?.onDone?.invoke()
                    pendingSpeech = null
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceSpeechEngine", "TTS init exception", e)
            _state.value = _state.value.copy(ttsReady = false, statusMessage = "TTS 사용 불가")
        }
    }

    fun speak(
        text: String,
        locale: Locale = Locale.US,
        pitch: Float = 1.0f,
        rate: Float = 1.0f,
        onDone: (() -> Unit)? = null
    ) {
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }
        // Only stop STT without clearing the new TTS fallback cadence!
        stopListeningOnly(commitPartial = false)
        stopFallbackCadence()
        val engine = tts
        if (engine == null || !_state.value.ttsReady) {
            pendingSpeech = PendingSpeechRequest(text, locale, pitch, rate, onDone)
            if (engine == null) initTts()
            return
        }
        val utteranceId = UUID.randomUUID().toString()
        activeUtteranceId = utteranceId
        onTtsDoneCallback = onDone
        currentUtteranceText = text
        currentUtteranceRate = rate.coerceIn(0.55f, 1.45f)

        val res = engine.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.setPitch(pitch.coerceIn(0.65f, 1.45f))
        engine.setSpeechRate(currentUtteranceRate)

        val code = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (code != TextToSpeech.SUCCESS) {
            activeUtteranceId = null
            _state.value = _state.value.copy(isSpeaking = false, spokenCharStart = -1, spokenCharEnd = -1)
            val cb = onTtsDoneCallback
            onTtsDoneCallback = null
            cb?.invoke()
        }
    }

    fun stopSpeaking() {
        pendingSpeech = null
        activeUtteranceId = null
        onTtsDoneCallback = null
        lastTtsStoppedAtMs = System.currentTimeMillis()
        runOnMain { stopFallbackCadence() }
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _state.value = _state.value.copy(isSpeaking = false, spokenCharStart = -1, spokenCharEnd = -1)
    }

    fun clearErrorNotice() {
        if (_state.value.errorNotice != null) {
            _state.value = _state.value.copy(errorNotice = null)
        }
    }

    private fun cancelWatchdog() {
        readinessWatchdog?.let { mainHandler.removeCallbacks(it) }
        readinessWatchdog = null
    }

    fun startListening(
        locale: Locale = Locale.US,
        onResult: (transcript: String, durationMs: Long) -> Unit
    ) {
        val wasSpeaking = _state.value.isSpeaking
        stopSpeaking()
        onFinalResultCallback = onResult
        latestCapturedPartial = ""

        val startTask = Runnable {
            cancelWatchdog()
            readyForSpeechReceived = false

            val hasMicPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMicPermission) {
                val msg = "마이크 권한을 허용해 주세요."
                _state.value = _state.value.copy(
                    isListening = false,
                    rmsDb = 0f,
                    statusMessage = msg,
                    errorNotice = msg,
                    errorTimestampMs = System.currentTimeMillis()
                )
                return@Runnable
            }

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                val msg = "실시간 마이크 미지원 기기 · 음성창 또는 키보드를 이용해 주세요."
                _state.value = _state.value.copy(
                    speechAvailable = false,
                    isListening = false,
                    statusMessage = msg,
                    errorNotice = msg,
                    errorTimestampMs = System.currentTimeMillis()
                )
                return@Runnable
            }

            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null

                _state.value = _state.value.copy(
                    isListening = true,
                    partialTranscript = "",
                    rmsDb = 1.5f,
                    errorNotice = null,
                    statusMessage = "듣는 중"
                )

                val watchdog = Runnable {
                    if (!readyForSpeechReceived && _state.value.isListening) {
                        val msg = "마이크 응답 지연 · 음성창 또는 키보드로 입력해 주세요."
                        try {
                            speechRecognizer?.cancel()
                            speechRecognizer?.destroy()
                            speechRecognizer = null
                        } catch (_: Exception) {
                        }
                        _state.value = _state.value.copy(
                            isListening = false,
                            rmsDb = 0f,
                            statusMessage = msg,
                            errorNotice = msg,
                            errorTimestampMs = System.currentTimeMillis()
                        )
                    }
                }
                readinessWatchdog = watchdog
                mainHandler.postDelayed(watchdog, 4000L)

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            readyForSpeechReceived = true
                            cancelWatchdog()
                            listenStartTimeMs = System.currentTimeMillis()
                            _state.value = _state.value.copy(
                                isListening = true,
                                partialTranscript = "",
                                rmsDb = 2.5f,
                                errorNotice = null,
                                statusMessage = "말씀하세요"
                            )
                        }

                        override fun onBeginningOfSpeech() {
                            readyForSpeechReceived = true
                            cancelWatchdog()
                            _state.value = _state.value.copy(statusMessage = "음성 인식 중")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            _state.value = _state.value.copy(rmsDb = rmsdB.coerceIn(0f, 12f))
                        }

                        override fun onBufferReceived(buffer: ByteArray?) = Unit

                        override fun onEndOfSpeech() {
                            _state.value = _state.value.copy(
                                isListening = false,
                                rmsDb = 0f,
                                statusMessage = "분석 중"
                            )
                        }

                        override fun onError(error: Int) {
                            cancelWatchdog()
                            // Recover captured partial speech if recognizer timed out right after speech!
                            val recovered = latestCapturedPartial.trim()
                            if ((error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) &&
                                recovered.isNotBlank()
                            ) {
                                latestCapturedPartial = ""
                                val duration = (System.currentTimeMillis() - listenStartTimeMs).coerceAtLeast(1000L)
                                _state.value = _state.value.copy(
                                    isListening = false,
                                    rmsDb = 0f,
                                    finalTranscript = recovered,
                                    partialTranscript = recovered,
                                    errorNotice = null,
                                    statusMessage = "인식 완료"
                                )
                                val cb = onFinalResultCallback
                                onFinalResultCallback = null
                                cb?.invoke(recovered, duration)
                                return
                            }

                            val msg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "잘 들리지 않았어요. 다시 말씀해 주세요."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성이 감지되지 않았습니다."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요합니다."
                                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 지연 · 음성창 또는 키보드를 이용해 주세요."
                                SpeechRecognizer.ERROR_AUDIO -> "마이크 입력 오류 · 음성창을 이용해 주세요."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "잠시 후 다시 눌러 주세요."
                                else -> "음성 인식 오류 (코드 $error)"
                            }
                            _state.value = _state.value.copy(
                                isListening = false,
                                rmsDb = 0f,
                                statusMessage = msg,
                                errorNotice = msg,
                                errorTimestampMs = System.currentTimeMillis()
                            )
                        }

                        override fun onResults(results: Bundle?) {
                            cancelWatchdog()
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val best = matches?.firstOrNull().orEmpty().trim()
                                .ifBlank { latestCapturedPartial.trim() }
                            latestCapturedPartial = ""
                            val duration = (System.currentTimeMillis() - listenStartTimeMs).coerceAtLeast(1000L)
                            _state.value = _state.value.copy(
                                isListening = false,
                                rmsDb = 0f,
                                finalTranscript = best,
                                partialTranscript = best,
                                errorNotice = if (best.isBlank()) "음성이 인식되지 않았습니다." else null,
                                statusMessage = if (best.isNotBlank()) "인식 완료" else "다시 말씀해 주세요"
                            )
                            if (best.isNotBlank()) {
                                val cb = onFinalResultCallback
                                onFinalResultCallback = null
                                cb?.invoke(best, duration)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                            if (partial.isNotBlank()) {
                                latestCapturedPartial = partial
                                _state.value = _state.value.copy(partialTranscript = partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    val tag = locale.toLanguageTag().ifBlank { "en-US" }
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1600L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                cancelWatchdog()
                Log.e("VoiceSpeechEngine", "Failed to start speech recognizer", e)
                val msg = "마이크 시작 오류 · 키보드 또는 음성창을 이용해 주세요."
                _state.value = _state.value.copy(
                    isListening = false,
                    rmsDb = 0f,
                    statusMessage = msg,
                    errorNotice = msg,
                    errorTimestampMs = System.currentTimeMillis()
                )
            }
        }

        cancelDelayedListen()
        delayedListenRunnable = startTask
        // 140ms speaker-drain guard if TTS was just active so mic never picks up residual TTS echo
        val elapsedSinceTts = System.currentTimeMillis() - lastTtsStoppedAtMs
        val delayMs = if (wasSpeaking || elapsedSinceTts < 140L) 140L else 0L
        if (delayMs > 0L) {
            mainHandler.postDelayed(startTask, delayMs)
        } else {
            runOnMain { startTask.run() }
        }
    }

    private fun stopListeningOnly(commitPartial: Boolean) {
        runOnMain {
            cancelDelayedListen()
            cancelWatchdog()
            val captured = latestCapturedPartial.trim()
            latestCapturedPartial = ""
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {
            }
            _state.value = _state.value.copy(isListening = false, rmsDb = 0f)
            if (commitPartial && captured.isNotBlank()) {
                val duration = (System.currentTimeMillis() - listenStartTimeMs).coerceAtLeast(1000L)
                val cb = onFinalResultCallback
                onFinalResultCallback = null
                cb?.invoke(captured, duration)
            }
        }
    }

    fun stopListening() {
        stopListeningOnly(commitPartial = true)
    }

    fun clearTranscript() {
        latestCapturedPartial = ""
        _state.value = _state.value.copy(partialTranscript = "", finalTranscript = "")
    }

    fun release() {
        pendingSpeech = null
        activeUtteranceId = null
        onTtsDoneCallback = null
        onFinalResultCallback = null
        runOnMain {
            cancelDelayedListen()
            cancelWatchdog()
            stopFallbackCadence()
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {
            }
        }
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {
        }
    }
}
