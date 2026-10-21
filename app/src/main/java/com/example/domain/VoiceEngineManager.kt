package com.example.domain

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SttState {
    IDLE,
    WAKE_LISTENING,
    CAPTURING_COMMAND,
    PROCESSING_AI,
    SPEAKING_RESPONSE
}

class VoiceEngineManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _sttState = MutableStateFlow(SttState.IDLE)
    val sttState: StateFlow<SttState> = _sttState.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _preferredSttLocale = MutableStateFlow("hi-IN") // Hindi-first preference
    val preferredSttLocale: StateFlow<String> = _preferredSttLocale.asStateFlow()

    private val _ttsActiveLanguage = MutableStateFlow("hi-IN")
    val ttsActiveLanguage: StateFlow<String> = _ttsActiveLanguage.asStateFlow()

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    tts?.language = Locale("hi", "IN")
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    fun setPreferredSttLocale(localeTag: String) {
        _preferredSttLocale.value = localeTag
    }

    fun playWakeChime() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
        } catch (_: Exception) {
            // Ignore audio hardware limitations in headless environments
        }
    }

    /**
     * Bilingual Offline TTS:
     * Evaluates Unicode script (Devanagari \u0900-\u097F vs Latin) and switches Locale("hi", "IN") or Locale("en", "IN").
     */
    fun speakBilingual(text: String) {
        val containsDevanagari = text.any { it in '\u0900'..'\u097F' }
        val targetLocale = if (containsDevanagari) {
            Locale("hi", "IN")
        } else {
            Locale("en", "IN")
        }
        _ttsActiveLanguage.value = if (containsDevanagari) "hi-IN (हिंदी)" else "en-IN (English)"

        if (!isTtsReady) return
        try {
            tts?.language = targetLocale
            tts?.setSpeechRate(0.98f)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gandhi_utt_${System.currentTimeMillis()}")
        } catch (_: Exception) {
            // Safe fallback if TTS voice pack is unavailable
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    fun startOfflineSpeechCapture(
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        playWakeChime()
        _sttState.value = SttState.CAPTURING_COMMAND
        _partialTranscript.value = ""

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Offline SpeechRecognizer hardware service unavailable on this emulator; use quick voice presets or text bar below.")
            _sttState.value = SttState.IDLE
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _sttState.value = SttState.CAPTURING_COMMAND
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _sttState.value = SttState.PROCESSING_AI
                    }

                    override fun onError(error: Int) {
                        _sttState.value = SttState.IDLE
                        onError("Offline STT completed or no microphone stream detected (Code $error).")
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.firstOrNull()?.trim().orEmpty()
                        _sttState.value = SttState.IDLE
                        if (spokenText.isNotEmpty()) {
                            onFinalResult(spokenText)
                        } else {
                            onError("No voice frames detected.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        if (partial.isNotEmpty()) {
                            _partialTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, _preferredSttLocale.value)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            _sttState.value = SttState.IDLE
            onError("Could not start offline STT: ${e.localizedMessage}")
        }
    }

    fun stopSpeechCapture() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {
        }
        _sttState.value = SttState.IDLE
    }

    fun release() {
        stopSpeechCapture()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}
