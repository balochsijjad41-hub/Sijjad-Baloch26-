package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class JarvisSpeechManager(
    private val context: Context,
    private val onRmsChanged: (Float) -> Unit,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    var isListening = false
        private set

    var continuousMode = false
    var wakeWordEnabled = false
    var preferredLocale: String = "auto" // auto, en-US, ur-PK, hi-IN

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        } else {
            onError("Speech recognition not available on this device.")
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            val locale = when (preferredLocale) {
                "ur-PK" -> Locale.forLanguageTag("ur-PK")
                "hi-IN" -> Locale.forLanguageTag("hi-IN")
                "en-US" -> Locale.US
                else -> Locale.getDefault()
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
            // Extra support for multilingual Urdu, Hindi, English
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("en-US", "ur-PK", "hi-IN"))
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            onListeningStateChanged(true)
        } catch (e: Exception) {
            Log.e("JarvisSpeechManager", "Failed to start listening", e)
            isListening = false
            onListeningStateChanged(false)
            onError(e.message ?: "Failed to start microphone.")
        }
    }

    fun stopListening() {
        isListening = false
        onListeningStateChanged(false)
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
    }

    fun cancelListening() {
        isListening = false
        onListeningStateChanged(false)
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
    }

    fun destroy() {
        isListening = false
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onListeningStateChanged(true)
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB ranges typically from -2 to 10
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                onRmsChanged(normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                onListeningStateChanged(false)
            }

            override fun onError(error: Int) {
                isListening = false
                onListeningStateChanged(false)
                onRmsChanged(0f)

                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client speech error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection required for voice"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timed out"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition engine busy"
                    SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input received"
                    else -> "Speech recognition error ($error)"
                }

                // If continuous mode or wake word is on and it's just timeout/no match, silently restart
                if ((error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) &&
                    (continuousMode || wakeWordEnabled)
                ) {
                    startListening()
                } else if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    onError(message)
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                onListeningStateChanged(false)
                onRmsChanged(0f)

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim().orEmpty()

                if (recognizedText.isNotBlank()) {
                    if (wakeWordEnabled) {
                        val lower = recognizedText.lowercase()
                        if (lower.contains("hey jarvis") || lower.contains("jarvis") || lower.contains("suno jarvis")) {
                            val clean = recognizedText
                                .replace("hey jarvis", "", true)
                                .replace("jarvis", "", true)
                                .replace("suno jarvis", "", true)
                                .trim()
                            if (clean.isNotBlank()) {
                                onFinalResult(clean)
                            } else {
                                onFinalResult("Yes, Sir?")
                            }
                        } else if (continuousMode) {
                            onFinalResult(recognizedText)
                        } else {
                            // If wake word enabled but doesn't contain wake word, resume listening
                            startListening()
                        }
                    } else {
                        onFinalResult(recognizedText)
                    }
                } else if (continuousMode) {
                    startListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull().orEmpty()
                if (partial.isNotBlank()) {
                    onPartialResult(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
