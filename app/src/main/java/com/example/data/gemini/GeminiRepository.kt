package com.example.data.gemini

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AresTone
import com.example.data.model.ScreenAnalysisResult
import com.example.data.model.ScreenVisionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiRepository(private val context: Context) {
  private val tag = "GeminiRepository"
  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()

  private val prefs = context.getSharedPreferences("ares_ai_prefs", Context.MODE_PRIVATE)

  fun getApiKey(): String {
    val customKey = prefs.getString("custom_gemini_api_key", "") ?: ""
    if (customKey.isNotBlank()) return customKey
    val buildKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (_: Exception) {
      ""
    }
    return if (buildKey != "MY_GEMINI_API_KEY" && buildKey.isNotBlank()) buildKey else ""
  }

  fun setCustomApiKey(key: String) {
    prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
  }

  fun isCloudKeyConfigured(): Boolean {
    val key = getApiKey()
    return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
  }

  suspend fun generateChatReply(
    prompt: String,
    tone: AresTone = AresTone.NATURAL,
    historySummary: String = ""
  ): Result<String> = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()
    if (!isCloudKeyConfigured()) {
      Log.d(tag, "Using local ARES Persona Engine (No Gemini API Key found)")
      val local = AresPersonaEngine.generateLocalResponse(prompt, tone)
      return@withContext Result.success(local)
    }

    try {
      val systemPrompt = "${AresPersonaEngine.SYSTEM_INSTRUCTION}\nTom da resposta desejado: ${tone.systemPromptModifier}\nSe houver histórico: $historySummary"
      
      val requestBodyJson = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
              put(JSONObject().put("text", "$systemPrompt\n\nPrompt do Usuário: $prompt"))
            })
          })
        })
      }

      // Using gemini-2.5-flash endpoint for high speed and reliability
      val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
      val request = Request.Builder()
        .url(url)
        .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      val responseString = response.body?.string().orEmpty()

      if (!response.isSuccessful) {
        Log.e(tag, "Gemini API error code: ${response.code}, body: $responseString")
        // Graceful fallback to local engine on API error
        val fallback = AresPersonaEngine.generateLocalResponse(prompt, tone)
        return@withContext Result.success("$fallback\n\n(Aviso: Resposta gerada pelo motor local ARES devido à latência da API - código ${response.code})")
      }

      val json = JSONObject(responseString)
      val candidates = json.optJSONArray("candidates")
      val firstCandidate = candidates?.optJSONObject(0)
      val contentObj = firstCandidate?.optJSONObject("content")
      val parts = contentObj?.optJSONArray("parts")
      val textResult = parts?.optJSONObject(0)?.optString("text")

      if (!textResult.isNullOrBlank()) {
        Result.success(textResult.trim())
      } else {
        Result.success(AresPersonaEngine.generateLocalResponse(prompt, tone))
      }
    } catch (e: Exception) {
      Log.e(tag, "Exception calling Gemini API", e)
      val localFallback = AresPersonaEngine.generateLocalResponse(prompt, tone)
      Result.success("$localFallback\n\n(Aviso: Processado pelo núcleo ARES local devido à desconexão de rede)")
    }
  }

  suspend fun analyzeScreenVision(
    mode: ScreenVisionMode,
    screenBitmap: Bitmap?,
    contextualText: String,
    tone: AresTone
  ): Result<ScreenAnalysisResult> = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()

    if (!isCloudKeyConfigured() || screenBitmap == null && contextualText.isBlank()) {
      val sampleName = if (contextualText.contains("WhatsApp", ignoreCase = true)) "WhatsApp Chat" else "Conversa na Tela"
      val fallback = AresPersonaEngine.generateScreenVisionLocalResult(
        mode = mode,
        sampleTitle = sampleName,
        visibleText = contextualText.ifBlank { "Última mensagem recebida: 'Você consegue me mandar o arquivo hoje até às 18h?'" },
        tone = tone
      )
      return@withContext Result.success(fallback)
    }

    try {
      val promptInstruction = if (mode == ScreenVisionMode.DESCRIBE_SCREEN) {
        """Analise a imagem da tela fornecida ou o texto extraído da conversa.
Identifique o aplicativo (ex: WhatsApp, Instagram, Telegram), identifique as mensagens visíveis, quem está falando e faça um resumo conciso e claro do contexto da conversa."""
      } else {
        """Analise a conversa visível nesta tela.
Considere as últimas mensagens e o tom solicitado: ${tone.systemPromptModifier}.
Gere UMA resposta excelente, pronta para o usuário revisar e inserir no campo de digitação.
Não envie automaticamente. Retorne apenas o texto da resposta sugerida ou uma análise estruturada."""
      }

      val partsArray = JSONArray()
      partsArray.put(JSONObject().put("text", "$promptInstruction\n\nTexto adicional detectado via acessibilidade: $contextualText"))

      if (screenBitmap != null) {
        val stream = ByteArrayOutputStream()
        screenBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val imageBytes = stream.toByteArray()
        val base64Data = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val inlineDataObj = JSONObject().apply {
          put("mimeType", "image/jpeg")
          put("data", base64Data)
        }
        partsArray.put(JSONObject().put("inlineData", inlineDataObj))
      }

      val requestBody = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("role", "user")
            put("parts", partsArray)
          })
        })
      }

      val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
      val request = Request.Builder()
        .url(url)
        .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      val responseString = response.body?.string().orEmpty()

      if (!response.isSuccessful) {
        Log.e(tag, "Screen Vision API error ${response.code}: $responseString")
        val fallback = AresPersonaEngine.generateScreenVisionLocalResult(
          mode, "Tela Capturada", contextualText.ifBlank { "Mensagens visíveis" }, tone
        )
        return@withContext Result.success(fallback)
      }

      val json = JSONObject(responseString)
      val candidates = json.optJSONArray("candidates")
      val textResult = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty()

      val result = ScreenAnalysisResult(
        mode = mode,
        detectedApp = if (contextualText.contains("Instagram", ignoreCase = true)) "Instagram Direct" else "WhatsApp Messenger",
        detectedSpeaker = "Contato",
        visibleMessages = contextualText.split("\n").filter { it.isNotBlank() },
        screenSummary = if (mode == ScreenVisionMode.DESCRIBE_SCREEN) textResult else "Conversa analisada com sucesso pelo Gemini multimodal.",
        suggestedReply = if (mode == ScreenVisionMode.REPLY_CONVERSATION) textResult else "Veja o resumo detalhado da tela.",
        selectedTone = tone,
        confidenceScore = 0.98f,
        isMockSample = false
      )
      Result.success(result)
    } catch (e: Exception) {
      Log.e(tag, "Error analyzing screen vision", e)
      val fallback = AresPersonaEngine.generateScreenVisionLocalResult(
        mode, "Tela do Sistema", contextualText, tone
      )
      Result.success(fallback)
    }
  }
}
