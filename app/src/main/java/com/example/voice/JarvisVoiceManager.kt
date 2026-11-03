package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.VoicePersona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "JarvisVoiceManager"

class JarvisVoiceManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _voiceRmsLevel = MutableStateFlow(0f)
    val voiceRmsLevel: StateFlow<Float> = _voiceRmsLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _voiceStatusMessage = MutableStateFlow<String?>(null)
    val voiceStatusMessage: StateFlow<String?> = _voiceStatusMessage.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val uzLocale = Locale.forLanguageTag("uz-UZ")
                    val trLocale = Locale.forLanguageTag("tr-TR")
                    val engine = tts
                    val result = engine?.setLanguage(uzLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        val fallbackResult = engine?.setLanguage(trLocale)
                        if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            engine?.language = Locale.US
                        }
                    }
                    engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                    })
                    isTtsReady = true
                } else {
                    Log.w(TAG, "TTS initialization returned status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    fun speak(text: String, persona: VoicePersona = VoicePersona.TACTICAL) {
        if (text.isBlank()) return
        if (!isTtsReady) {
            _voiceStatusMessage.value = "Ovoz sintezatori (TTS) tayyorlanmoqda..."
            return
        }
        try {
            tts?.setPitch(persona.pitch)
            tts?.setSpeechRate(persona.speechRate)
            val cleanText = text.replace("*", "").replace("#", "").trim()
            val params = Bundle()
            val result = tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "jarvis_utt_${System.currentTimeMillis()}")
            if (result == TextToSpeech.SUCCESS) {
                _isSpeaking.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
            _isSpeaking.value = false
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        } finally {
            _isSpeaking.value = false
        }
    }

    fun startListening(onFinalResult: (String) -> Unit) {
        stopSpeaking()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceStatusMessage.value = "Ushbu qurilmada SpeechRecognizer xizmati topilmadi. Tezkor ovozli buyruq tugmalaridan foydalaning."
            return
        }
        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _voiceStatusMessage.value = "Jarvis tinglamoqda... Gapiring!"
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                    _voiceRmsLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _voiceRmsLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _voiceRmsLevel.value = 0f
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Ovoz aniqlanmadi. Qaytadan gapiring yoki tezkor buyruqni tanlang."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Ovoz kiritilmadi."
                        SpeechRecognizer.ERROR_AUDIO -> "Mikrofon audio xatosi."
                        else -> "Ovozli aniqlash tugadi (kod: $error). Tezkor ovozli buyruqlardan foydalanishingiz mumkin."
                    }
                    _voiceStatusMessage.value = msg
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _voiceRmsLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val bestMatch = matches?.firstOrNull().orEmpty()
                    _partialTranscript.value = ""
                    if (bestMatch.isNotBlank()) {
                        _voiceStatusMessage.value = null
                        onFinalResult(bestMatch)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uz-UZ")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _isListening.value = false
            _voiceStatusMessage.value = "Mikrofonni ishga tushirishda xatolik: ${e.localizedMessage}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SpeechRecognizer", e)
        } finally {
            _isListening.value = false
            _voiceRmsLevel.value = 0f
        }
    }

    fun clearStatusMessage() {
        _voiceStatusMessage.value = null
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing JarvisVoiceManager", e)
        }
    }
}
