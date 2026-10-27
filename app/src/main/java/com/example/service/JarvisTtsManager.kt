package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class JarvisTtsManager(
    private val context: Context,
    private val onStartSpeaking: () -> Unit = {},
    private val onDoneSpeaking: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var speechRate: Float = 1.05f
    var speechPitch: Float = 0.95f // Slightly deeper, sophisticated JARVIS tone

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(speechPitch)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onStartSpeaking()
                }

                override fun onDone(utteranceId: String?) {
                    onDoneSpeaking()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onError("TTS speech error")
                }
            })
        } else {
            isInitialized = false
            Log.e("JarvisTtsManager", "TTS initialization failed status=$status")
        }
    }

    fun speak(text: String) {
        if (!isInitialized || text.isBlank()) return

        // Auto-select language based on text script
        val locale = detectLocaleForText(text)
        try {
            val res = tts?.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to UK/US English with Jarvis tone
                tts?.setLanguage(Locale.UK)
            }
        } catch (_: Exception) {
            tts?.setLanguage(Locale.US)
        }

        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)

        val utteranceId = "jarvis_reply_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }

    private fun detectLocaleForText(text: String): Locale {
        for (char in text) {
            val code = char.code
            // Arabic / Urdu unicode range
            if (code in 0x0600..0x06FF || code in 0x0750..0x077F) {
                return Locale.forLanguageTag("ur-PK")
            }
            // Devanagari / Hindi unicode range
            if (code in 0x0900..0x097F) {
                return Locale.forLanguageTag("hi-IN")
            }
        }
        // Default to UK English for authentic Jarvis British accent
        return Locale.UK
    }
}
