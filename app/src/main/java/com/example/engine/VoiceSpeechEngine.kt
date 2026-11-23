package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
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
    val statusMessage: String = "준비 완료"
)

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

    private var onFinalResultCallback: ((transcript: String, durationMs: Long) -> Unit)? = null
    private var onTtsDoneCallback: (() -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = true,
                            statusMessage = "원어민 선생님 발화 중..."
                        )
                    }

                    override fun onDone(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            statusMessage = "듣기 완료"
                        )
                        val cb = onTtsDoneCallback
                        onTtsDoneCallback = null
                        if (cb != null) {
                            mainHandler.post { cb() }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            statusMessage = "음성 출력 오류"
                        )
                        onTtsDoneCallback = null
                    }
                })
                _state.value = _state.value.copy(ttsReady = true, statusMessage = "음성 엔진 준비 완료")
            } else {
                Log.w("VoiceSpeechEngine", "TTS init failed with status=$status")
                _state.value = _state.value.copy(ttsReady = false, statusMessage = "TTS 초기화 대기")
            }
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
        stopListening()
        val engine = tts
        if (engine == null || !_state.value.ttsReady) {
            onDone?.invoke()
            return
        }
        onTtsDoneCallback = onDone
        val res = engine.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.setPitch(pitch.coerceIn(0.65f, 1.45f))
        engine.setSpeechRate(rate.coerceIn(0.55f, 1.45f))
        val utteranceId = UUID.randomUUID().toString()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        onTtsDoneCallback = null
        tts?.stop()
        _state.value = _state.value.copy(isSpeaking = false)
    }

    fun startListening(
        locale: Locale = Locale.US,
        onResult: (transcript: String, durationMs: Long) -> Unit
    ) {
        stopSpeaking()
        onFinalResultCallback = onResult

        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _state.value = _state.value.copy(
                    speechAvailable = false,
                    isListening = false,
                    statusMessage = "이 기기에서는 실시간 음성 인식이 제한되어 텍스트 입력 모드를 함께 지원합니다."
                )
                return@post
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            listenStartTimeMs = System.currentTimeMillis()
                            _state.value = _state.value.copy(
                                isListening = true,
                                partialTranscript = "",
                                rmsDb = 2f,
                                statusMessage = "지금 자연스럽게 영어로 말해 보세요!"
                            )
                        }

                        override fun onBeginningOfSpeech() {
                            _state.value = _state.value.copy(statusMessage = "실시간 음성 분석 중...")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            _state.value = _state.value.copy(rmsDb = rmsdB.coerceIn(0f, 12f))
                        }

                        override fun onBufferReceived(buffer: ByteArray?) = Unit

                        override fun onEndOfSpeech() {
                            _state.value = _state.value.copy(
                                isListening = false,
                                rmsDb = 0f,
                                statusMessage = "발음 정밀 분석 중..."
                            )
                        }

                        override fun onError(error: Int) {
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "잘 들리지 않았어요. 한 번 더 또렷하게 말해 주세요!"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 시간이 초과되었어요. 마이크를 다시 눌러 주세요."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요합니다."
                                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 지연이 있습니다. 텍스트 입력 또는 재시도를 이용해 주세요."
                                else -> "음성 인식을 다시 시도해 주세요 (코드 $error)"
                            }
                            _state.value = _state.value.copy(
                                isListening = false,
                                rmsDb = 0f,
                                statusMessage = msg
                            )
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val best = matches?.firstOrNull().orEmpty().trim()
                            val duration = (System.currentTimeMillis() - listenStartTimeMs).coerceAtLeast(1000L)
                            _state.value = _state.value.copy(
                                isListening = false,
                                rmsDb = 0f,
                                finalTranscript = best,
                                partialTranscript = best,
                                statusMessage = if (best.isNotBlank()) "인식 완료" else "다시 말해 보세요"
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
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("VoiceSpeechEngine", "Failed to start speech recognizer", e)
                _state.value = _state.value.copy(
                    isListening = false,
                    statusMessage = "마이크 시작 오류: 텍스트 입력으로도 연습할 수 있습니다."
                )
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {
            }
            _state.value = _state.value.copy(isListening = false, rmsDb = 0f)
        }
    }

    fun clearTranscript() {
        _state.value = _state.value.copy(partialTranscript = "", finalTranscript = "")
    }

    fun release() {
        onTtsDoneCallback = null
        onFinalResultCallback = null
        mainHandler.post {
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
