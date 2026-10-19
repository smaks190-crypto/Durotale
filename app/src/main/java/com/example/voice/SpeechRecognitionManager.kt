package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechRecognitionManager(
    private val context: Context,
    private val onRmsChanged: (Float) -> Unit,
    private val onPartialText: (String) -> Unit,
    private val onFinalText: (String) -> Unit,
    private val onStatusChanged: (isListening: Boolean) -> Unit,
    private val onError: (String) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isSessionActive = false
    private var isCurrentlyListening = false

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (!isAvailable()) {
            onError("Распознавание речи недоступно на данном устройстве")
            return
        }

        isSessionActive = true
        mainHandler.post {
            initRecognizerIfNeeded()
            beginListeningInternal()
        }
    }

    fun stopListening() {
        isSessionActive = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recognizer: ${e.message}")
            }
            isCurrentlyListening = false
            onStatusChanged(false)
            onRmsChanged(0f)
        }
    }

    fun destroy() {
        isSessionActive = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying recognizer: ${e.message}")
            }
            speechRecognizer = null
            isCurrentlyListening = false
            onStatusChanged(false)
        }
    }

    private fun initRecognizerIfNeeded() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@SpeechRecognitionManager)
            }
        }
    }

    private fun beginListeningInternal() {
        if (!isSessionActive) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            isCurrentlyListening = true
            onStatusChanged(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
            scheduleRestart(500)
        }
    }

    private fun scheduleRestart(delayMillis: Long) {
        if (!isSessionActive) return
        mainHandler.postDelayed({
            if (isSessionActive) {
                initRecognizerIfNeeded()
                beginListeningInternal()
            }
        }, delayMillis)
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        isCurrentlyListening = true
        onStatusChanged(true)
    }

    override fun onBeginningOfSpeech() {
        isCurrentlyListening = true
        onStatusChanged(true)
    }

    override fun onRmsChanged(rmsdB: Float) {
        if (!isSessionActive) {
            onRmsChanged(0f)
            return
        }
        // Normalize rmsdB (-2 to 10 typical range) to 0.0f..1.0f
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        onRmsChanged(normalized)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        // Microphone paused speaking, but if user is still in voice session, we restart listening seamlessly
        if (isSessionActive) {
            scheduleRestart(250)
        } else {
            isCurrentlyListening = false
            onStatusChanged(false)
            onRmsChanged(0f)
        }
    }

    override fun onError(error: Int) {
        Log.d(TAG, "SpeechRecognizer error: $error")
        onRmsChanged(0f)

        // Errors like ERROR_NO_MATCH, ERROR_SPEECH_TIMEOUT occur naturally on silent pauses.
        // If the session is active, silently restart to keep listening.
        if (isSessionActive) {
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_CLIENT -> {
                    scheduleRestart(300)
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    isSessionActive = false
                    isCurrentlyListening = false
                    onStatusChanged(false)
                    onError("Требуется разрешение на запись аудио")
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    try {
                        speechRecognizer?.cancel()
                    } catch (_: Exception) {}
                    scheduleRestart(400)
                }
                else -> {
                    scheduleRestart(500)
                }
            }
        } else {
            isCurrentlyListening = false
            onStatusChanged(false)
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim().orEmpty()
        if (text.isNotBlank()) {
            onFinalText(text)
        }
        if (isSessionActive) {
            scheduleRestart(200)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim().orEmpty()
        if (text.isNotBlank()) {
            onPartialText(text)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    companion object {
        private const val TAG = "SpeechRecognitionMgr"
    }
}
