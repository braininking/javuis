package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed interface VoiceState {
  data object Idle : VoiceState
  data object Ready : VoiceState
  data class Listening(val rmsDb: Float) : VoiceState
  data class Success(val recognizedText: String) : VoiceState
  data class Error(val message: String) : VoiceState
}

class AresVoiceManager(private val context: Context) {
  private val tag = "AresVoiceManager"
  private var speechRecognizer: SpeechRecognizer? = null

  private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
  val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

  fun isRecognitionAvailable(): Boolean {
    return SpeechRecognizer.isRecognitionAvailable(context)
  }

  fun startListening() {
    try {
      if (!isRecognitionAvailable()) {
        _voiceState.value = VoiceState.Error("Reconhecimento de voz não suportado neste dispositivo.")
        return
      }

      stopListening()

      speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.value = VoiceState.Ready
          }

          override fun onBeginningOfSpeech() {
            _voiceState.value = VoiceState.Listening(0f)
          }

          override fun onRmsChanged(rmsdB: Float) {
            _voiceState.value = VoiceState.Listening(rmsdB)
          }

          override fun onBufferReceived(buffer: ByteArray?) {}
          override fun onEndOfSpeech() {}

          override fun onError(error: Int) {
            val message = when (error) {
              SpeechRecognizer.ERROR_NO_MATCH -> "Nenhuma fala compreendida. Tente falar mais perto."
              SpeechRecognizer.ERROR_NETWORK -> "Erro de rede no reconhecimento de voz."
              SpeechRecognizer.ERROR_AUDIO -> "Erro de gravação de áudio."
              SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone necessária."
              else -> "ARES Voice em espera ($error)."
            }
            Log.w(tag, "Speech recognition error: $error ($message)")
            _voiceState.value = VoiceState.Error(message)
          }

          override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
              _voiceState.value = VoiceState.Success(text)
            } else {
              _voiceState.value = VoiceState.Idle
            }
          }

          override fun onPartialResults(partialResults: Bundle?) {}
          override fun onEvent(eventType: Int, params: Bundle?) {}
        })
      }

      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "pt-BR")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
      }

      speechRecognizer?.startListening(intent)
    } catch (e: Exception) {
      Log.e(tag, "Failed to start listening", e)
      _voiceState.value = VoiceState.Error(e.localizedMessage ?: "Falha ao iniciar microfone")
    }
  }

  fun stopListening() {
    try {
      speechRecognizer?.stopListening()
      speechRecognizer?.destroy()
    } catch (_: Exception) {}
    speechRecognizer = null
    if (_voiceState.value is VoiceState.Listening || _voiceState.value is VoiceState.Ready) {
      _voiceState.value = VoiceState.Idle
    }
  }
}
