package com.example.keyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.os.Bundle
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import android.inputmethodservice.InputMethodService
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.data.gemini.AresPersonaEngine
import com.example.data.gemini.GeminiRepository
import com.example.data.model.AresTone
import com.example.service.AresContextBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AresInputMethodService : InputMethodService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private lateinit var geminiRepository: GeminiRepository
  private var isShifted = false
  private var isSymbols = false
  private var speechRecognizer: SpeechRecognizer? = null
  private var translator: com.google.mlkit.nl.translate.Translator? = null
  private var auxiliaryPanel: TextView? = null
  private var showingGifPanel = false

  private lateinit var keyboardRoot: LinearLayout
  private lateinit var keysContainer: LinearLayout
  private lateinit var aiSuggestionBar: LinearLayout
  private lateinit var aiStatusText: TextView
  private lateinit var aiActionBtn: TextView

  private var currentAiSuggestion: String = ""

  override fun onCreate() {
    super.onCreate()
    geminiRepository = GeminiRepository(this)
    translator = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.PORTUGUESE).build())
  }

  override fun onDestroy() {
    super.onDestroy()
    speechRecognizer?.destroy()
    speechRecognizer = null
    translator?.close()
    translator = null
    serviceScope.cancel()
  }

  override fun onCreateInputView(): View {
    keyboardRoot = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setBackgroundColor(Color.parseColor("#090E18"))
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPadding(8, 6, 8, 12)
    }

    // 1. ARES AI Header Bar
    val aiHeader = buildAiAssistantBar()
    keyboardRoot.addView(aiHeader)

    // 2. Suggestion Preview Banner (expandable)
    aiSuggestionBar = buildAiSuggestionPreview()
    keyboardRoot.addView(aiSuggestionBar)

    // 3. Keys Container
    keysContainer = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }
    keyboardRoot.addView(keysContainer)

    renderKeys()
    return keyboardRoot
  }

  override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
    super.onStartInputView(info, restarting)
    // Check if there's any queued text from ARES Visão Inteligente
    val pending = AresContextBridge.consumePendingText()
    if (!pending.isNullOrBlank()) {
      currentAiSuggestion = pending
      showSuggestionPreview(pending, AresTone.NATURAL)
    }
  }

  private fun buildAiAssistantBar(): View {
    val bar = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dpToPx(42)
      ).apply { setMargins(4, 0, 4, 6) }
    }

    // ARES HUD Badge
    val aresBadge = TextView(this).apply {
      text = "⚡ ARES IA"
      setTextColor(Color.parseColor("#00E5FF"))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      typeface = Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
      background = GradientDrawable().apply {
        setColor(Color.parseColor("#102035"))
        setStroke(2, Color.parseColor("#00E5FF"))
        cornerRadius = dpToPx(14).toFloat()
      }
      setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
      setOnClickListener {
        triggerQuickAiSuggestion(AresTone.NATURAL)
      }
    }
    bar.addView(aresBadge)

    // Horizontal Scroll for Tones
    val scrollView = HorizontalScrollView(this).apply {
      isHorizontalScrollBarEnabled = false
      layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
        setMargins(dpToPx(6), 0, 0, 0)
      }
    }

    val tonesRow = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
    }

    AresTone.entries.forEach { tone ->
      val toneChip = TextView(this).apply {
        text = tone.displayName
        setTextColor(Color.parseColor("#C2D9F2"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
          setColor(Color.parseColor("#141E2F"))
          cornerRadius = dpToPx(12).toFloat()
        }
        setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
        layoutParams = LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dpToPx(4), 0, dpToPx(4), 0) }
        setOnClickListener {
          triggerQuickAiSuggestion(tone)
        }
      }
      tonesRow.addView(toneChip)
    }

    scrollView.addView(tonesRow)
    bar.addView(scrollView)
    return bar
  }

  private fun buildAiSuggestionPreview(): LinearLayout {
    val container = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      visibility = View.GONE
      background = GradientDrawable().apply {
        setColor(Color.parseColor("#111D30"))
        setStroke(1, Color.parseColor("#00B0FF"))
        cornerRadius = dpToPx(8).toFloat()
      }
      setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).apply { setMargins(4, 0, 4, 8) }
    }

    aiStatusText = TextView(this).apply {
      text = ""
      setTextColor(Color.WHITE)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      maxLines = 2
      layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    container.addView(aiStatusText)

    aiActionBtn = TextView(this).apply {
      text = "Inserir"
      setTextColor(Color.parseColor("#00E5FF"))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      typeface = Typeface.DEFAULT_BOLD
      background = GradientDrawable().apply {
        setColor(Color.parseColor("#0D3650"))
        cornerRadius = dpToPx(6).toFloat()
      }
      setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).apply { setMargins(dpToPx(6), 0, 0, 0) }
      setOnClickListener {
        if (currentAiSuggestion.isNotBlank()) {
          currentInputConnection?.commitText(currentAiSuggestion, 1)
          Toast.makeText(this@AresInputMethodService, "Sugestão inserida para revisão!", Toast.LENGTH_SHORT).show()
          container.visibility = View.GONE
        }
      }
    }
    container.addView(aiActionBtn)

    return container
  }

  private fun triggerQuickAiSuggestion(tone: AresTone) {
    aiSuggestionBar.visibility = View.VISIBLE
    aiStatusText.text = "⚡ ARES analisando contexto (${tone.displayName})..."
    aiActionBtn.visibility = View.GONE

    serviceScope.launch {
      val screenContext = AresContextBridge.latestScreenText.value
      val suggestion = withContext(Dispatchers.IO) {
        val prompt = if (screenContext.isNotBlank()) {
          "Com base na conversa atual: '$screenContext', formule uma resposta excelente no tom ${tone.displayName} para eu revisar e enviar."
        } else {
          "Formule uma resposta inteligente, cordial e bem articulada no tom ${tone.displayName} para mensagens cotidianas."
        }
        val result = geminiRepository.generateChatReply(prompt, tone)
        result.getOrDefault(AresPersonaEngine.generateLocalResponse(prompt, tone))
      }

      currentAiSuggestion = suggestion.replace("\n\n(Aviso:.*)".toRegex(), "").trim()
      showSuggestionPreview(currentAiSuggestion, tone)
    }
  }

  private fun showSuggestionPreview(text: String, tone: AresTone) {
    aiSuggestionBar.visibility = View.VISIBLE
    aiStatusText.text = "Sugestão (${tone.displayName}): \"$text\""
    aiActionBtn.visibility = View.VISIBLE
  }

  private fun renderKeys() {
    keysContainer.removeAllViews()

    val rows = if (isSymbols) {
      listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/"),
        listOf("SYM_SWITCH", "*", "\"", "'", ":", ";", "!", "?", "BACKSPACE"),
        listOf("ABC_SWITCH", ",", "SPACE", ".", "ENTER")
      )
    } else {
      listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "BACKSPACE"),
        listOf("?123", ",", "SPACE", ".", "ENTER")
      )
    }

    rows.forEach { rowKeys ->
      val rowLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          dpToPx(46)
        ).apply { setMargins(0, dpToPx(2), 0, dpToPx(2)) }
      }

      rowKeys.forEach { keyLabel ->
        val keyView = createKeyView(keyLabel)
        rowLayout.addView(keyView)
      }

      keysContainer.addView(rowLayout)
    }
  }

  private fun createKeyView(label: String): View {
    val keyBtn = TextView(this).apply {
      val displayText = when {
        label == "SPACE" -> "Espaço"
        label == "BACKSPACE" -> "⌫"
        label == "ENTER" -> "↵"
        label == "SHIFT" -> if (isShifted) "▲" else "⇧"
        label == "?123" || label == "SYM_SWITCH" -> "?123"
        label == "ABC_SWITCH" -> "ABC"
        isShifted -> label.uppercase()
        else -> label.lowercase()
      }

      text = displayText
      setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label == "SPACE") 12f else 16f)
      gravity = Gravity.CENTER
      typeface = Typeface.DEFAULT_BOLD

      val isSpecial = label in listOf("SHIFT", "BACKSPACE", "ENTER", "?123", "ABC_SWITCH", "SYM_SWITCH")
      val bgDrawable = GradientDrawable().apply {
        setColor(
          when {
            label == "ENTER" -> Color.parseColor("#007A99")
            isSpecial -> Color.parseColor("#19253C")
            else -> Color.parseColor("#152134")
          }
        )
        setStroke(1, Color.parseColor("#253754"))
        cornerRadius = dpToPx(6).toFloat()
      }
      background = bgDrawable

      setTextColor(
        when {
          label == "ENTER" -> Color.parseColor("#00E5FF")
          isSpecial -> Color.parseColor("#A3C2E6")
          else -> Color.WHITE
        }
      )

      val weight = when (label) {
        "SPACE" -> 4f
        "BACKSPACE", "SHIFT", "ENTER", "?123", "ABC_SWITCH", "SYM_SWITCH" -> 1.5f
        else -> 1f
      }

      layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight).apply {
        setMargins(dpToPx(2), 0, dpToPx(2), 0)
      }

      setOnClickListener {
        handleKeyPress(label)
      }
    }
    return keyBtn
  }

  private fun handleKeyPress(label: String) {
    val ic = currentInputConnection ?: return

    when (label) {
      "SPACE" -> ic.commitText(" ", 1)
      "BACKSPACE" -> ic.deleteSurroundingText(1, 0)
      "ENTER" -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
      "SHIFT" -> {
        isShifted = !isShifted
        renderKeys()
      }
      "?123", "SYM_SWITCH" -> {
        isSymbols = true
        renderKeys()
      }
      "ABC_SWITCH" -> {
        isSymbols = false
        renderKeys()
      }
      else -> {
        val charToCommit = if (isShifted) label.uppercase() else label.lowercase()
        ic.commitText(charToCommit, 1)
        if (isShifted) {
          isShifted = false
          renderKeys()
        }
      }
    }
  }

  private fun dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
  }
}
