package com.ghdinteractivestudio.codestation.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

sealed class TranscriptionResult {
    data class Partial(val text: String) : TranscriptionResult()
    data class Success(val text: String) : TranscriptionResult()
    data class Error(val message: String) : TranscriptionResult()
}

class VoiceTranscriptionService(private val context: Context) {

    private val tag = "VoiceTranscription"

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListeningFlow(): Flow<TranscriptionResult> = callbackFlow {
        if (!isAvailable()) {
            trySend(TranscriptionResult.Error("Reconnaissance vocale non disponible sur cet appareil."))
            close()
            return@callbackFlow
        }

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(tag, "onReadyForSpeech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(tag, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(tag, "onEndOfSpeech")
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Erreur d'enregistrement audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Erreur client"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions microphone manquantes"
                    SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Délai d'attente réseau dépassé"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Aucune parole détectée"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service de reconnaissance occupé"
                    SpeechRecognizer.ERROR_SERVER -> "Erreur serveur"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Aucun son détecté"
                    else -> "Erreur de reconnaissance vocale ($error)"
                }
                Log.w(tag, "SpeechRecognizer error: $errorMsg")
                trySend(TranscriptionResult.Error(errorMsg))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                Log.d(tag, "onResults text: $text")
                if (text.isNotBlank()) {
                    trySend(TranscriptionResult.Success(text))
                } else {
                    trySend(TranscriptionResult.Error("Aucune parole reconnue"))
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                if (text.isNotBlank()) {
                    trySend(TranscriptionResult.Partial(text))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(listener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            trySend(TranscriptionResult.Error("Impossible de démarrer l'écoute : ${e.message}"))
        }

        awaitClose {
            try {
                speechRecognizer.stopListening()
                speechRecognizer.destroy()
            } catch (_: Exception) {}
        }
    }
}
