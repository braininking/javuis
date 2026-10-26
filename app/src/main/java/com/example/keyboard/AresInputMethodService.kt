package com.example.keyboard

import android.graphics.Color
import android.graphics.Bitmap
import android.content.ComponentName
import com.example.service.AresInputCallback
import com.example.service.AresVisionConsentActivity
import com.example.data.model.ScreenVisionMode
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
import android.widget.FrameLayout
import android.widget.PopupWindow
import com.example.R
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.graphics.BitmapFactory
import android.app.AlertDialog
import android.view.inputmethod.InputMethodManager
import com.example.service.AresMediaInserter
import com.example.service.TenorApiService
import com.example.service.GifObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
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
  private var auxiliaryPanel: View? = null
  private var showingGifPanel = false
  private val tenorPreferences by lazy { getSharedPreferences("ares_tenor", MODE_PRIVATE) }
  private val tenorApi: TenorApiService by lazy {
    Retrofit.Builder()
      .baseUrl("https://tenor.googleapis.com/")
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(TenorApiService::class.java)
  }

  private lateinit var keyboardRoot: LinearLayout
  private lateinit var keysContainer: LinearLayout
  private lateinit var headerContainer: LinearLayout
  private lateinit var contentContainer: FrameLayout
  private lateinit var contentStack: LinearLayout
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
    keyboardRoot = layoutInflater.inflate(R.layout.ares_keyboard_main, null) as LinearLayout
    headerContainer = keyboardRoot.findViewById(R.id.ares_keyboard_header_container)
    contentContainer = keyboardRoot.findViewById(R.id.keyboard_content_container)
    contentStack = keyboardRoot.findViewById(R.id.keyboard_content_stack)
    keysContainer = keyboardRoot.findViewById(R.id.keyboard_keys_container)

    headerContainer.addView(buildAiAssistantBar(), LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48)
    ))

    aiSuggestionBar = buildAiSuggestionPreview()
    contentStack.addView(aiSuggestionBar, 0)

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
        dpToPx(48)
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
        launchVisionCapture(AresTone.NATURAL)
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

    bar.addView(createToolButton("文", "Traduzir inglês para português") { translateBeforeCursor() })
    bar.addView(createToolButton("☺", "Abrir emojis") { toggleEmojiPanel() })
    bar.addView(createToolButton("🎙", "Digitar por voz") { startVoiceInput() })
    bar.addView(createToolButton("GIF", "Abrir painel de GIFs") { toggleGifPanel() })
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

  private fun launchVisionCapture(tone: AresTone) {
    val contextText = AresContextBridge.latestScreenText.value
    aiSuggestionBar.visibility = View.VISIBLE
    aiStatusText.text = "ARES aguardando autorização para analisar a tela…"
    aiActionBtn.visibility = View.GONE
    AresInputCallback.onBitmap = { bitmap: Bitmap ->
      if (android.os.Build.VERSION.SDK_INT >= 28) requestShowSelf(0)
      serviceScope.launch {
        try {
          aiStatusText.text = "Analisando imagem e contexto com Gemini…"
          val result = withContext(Dispatchers.IO) {
            geminiRepository.analyzeScreenVision(ScreenVisionMode.REPLY_CONVERSATION, bitmap, contextText, tone)
          }
          currentAiSuggestion = result.getOrNull()?.suggestedReply.orEmpty().trim()
          if (currentAiSuggestion.isNotBlank()) showSuggestionPreview(currentAiSuggestion, tone)
          else aiStatusText.text = "Não foi possível gerar uma sugestão."
        } finally {
          if (!bitmap.isRecycled) bitmap.recycle()
        }
      }
    }
    AresInputCallback.onError = { message ->
      if (android.os.Build.VERSION.SDK_INT >= 28) requestShowSelf(0)
      aiStatusText.text = message
      aiActionBtn.visibility = View.GONE
    }
    try {
      startActivity(Intent(this, AresVisionConsentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
      AresInputCallback.onBitmap = null
      AresInputCallback.onError = null
      aiStatusText.text = "Não foi possível abrir a autorização de captura: " + (e.localizedMessage ?: "erro")
    }
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
          dpToPx(43)
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
      if (label.length == 1 && label[0].isLetter()) {
        setOnLongClickListener {
          showAccentPopup(it, label.lowercase())
          true
        }
      }
      if (label == "SPACE") {
        var downX = 0f
        var lastX = 0f
        var dragged = false
        var accumulated = 0f
        setOnTouchListener { _, event ->
          when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
              downX = event.rawX
              lastX = downX
              dragged = false
              accumulated = 0f
              false
            }
            android.view.MotionEvent.ACTION_MOVE -> {
              val dx = event.rawX - lastX
              if (kotlin.math.abs(event.rawX - downX) > dpToPx(8)) dragged = true
              if (dragged) {
                accumulated += dx
                lastX = event.rawX
                val step = dpToPx(12).coerceAtLeast(1)
                if (kotlin.math.abs(accumulated) >= step) {
                  val ic = currentInputConnection
                  val extracted = ic?.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                  if (ic != null && extracted != null) {
                    val textLength = extracted.text?.length ?: 0
                    val cursor = extracted.selectionStart.coerceIn(0, textLength)
                    val target = (cursor + if (accumulated > 0) 1 else -1).coerceIn(0, textLength)
                    ic.setSelection(target, target)
                  }
                  accumulated = 0f
                }
                true
              } else false
            }
            android.view.MotionEvent.ACTION_UP -> if (dragged) true else false
            else -> false
          }
        }
      } else if (label == "BACKSPACE") {
        var anchorX = 0f
        var dragDeleting = false
        val deleteThreshold = dpToPx(30).coerceAtLeast(1)
        setOnTouchListener { _, event ->
          when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
              anchorX = event.rawX
              dragDeleting = false
              false
            }
            android.view.MotionEvent.ACTION_MOVE -> {
              val movedLeft = anchorX - event.rawX
              if (movedLeft >= deleteThreshold) {
                // One deletion per 30dp crossed; never query/delete on every pixel.
                deleteWordsFromCursor(dpToPx(18).toFloat())
                anchorX = event.rawX
                dragDeleting = true
                true
              } else dragDeleting
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> dragDeleting
            else -> dragDeleting
          }
        }
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

  private fun createToolButton(label: String, description: String, action: () -> Unit): TextView {
    return TextView(this).apply {
      text = label
      contentDescription = description
      gravity = Gravity.CENTER
      setTextColor(Color.parseColor("#1A73E8"))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
      layoutParams = LinearLayout.LayoutParams(dpToPx(38), dpToPx(48))
      setOnClickListener { action() }
    }
  }

  private fun toggleEmojiPanel() {
    auxiliaryPanel?.let { contentStack.removeView(it) }
    keysContainer.visibility = View.GONE
    showingGifPanel = true
    auxiliaryPanel = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER
      layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
      val title = TextView(this@AresInputMethodService).apply {
        text = "Emojis — toque para inserir"
        setTextColor(Color.DKGRAY)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        gravity = Gravity.CENTER
      }
      addView(title)
      val emojis = listOf("😀", "😂", "🥹", "😍", "🥰", "😎", "😭", "🙏", "❤️", "👍", "🔥", "🎉")
      val rows = emojis.chunked(6)
      rows.forEach { row ->
        val line = LinearLayout(this@AresInputMethodService).apply {
          orientation = LinearLayout.HORIZONTAL
          gravity = Gravity.CENTER
        }
        row.forEach { emoji ->
          val item = TextView(this@AresInputMethodService).apply {
            text = emoji
            textSize = 25f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(48), 1f)
            setOnClickListener { currentInputConnection?.commitText(emoji, 1) }
          }
          line.addView(item)
        }
        addView(line)
      }
      val back = TextView(this@AresInputMethodService).apply {
        text = "Voltar ao teclado"
        gravity = Gravity.CENTER
        setTextColor(Color.parseColor("#1A73E8"))
        setPadding(0, dpToPx(8), 0, dpToPx(8))
        setOnClickListener { toggleGifPanel() }
      }
      addView(back)
    }
    contentStack.addView(auxiliaryPanel, 0)
  }

  private fun showAccentPopup(anchor: View, base: String) {
    val options = when (base) {
      "a" -> listOf("á", "à", "â", "ã", "ä")
      "e" -> listOf("é", "è", "ê", "ë")
      "i" -> listOf("í", "ì", "î", "ï")
      "o" -> listOf("ó", "ò", "ô", "õ", "ö")
      "u" -> listOf("ú", "ù", "û", "ü")
      "c" -> listOf("ç")
      "n" -> listOf("ñ")
      else -> return
    }
    var popup: PopupWindow? = null
    val row = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
      options.forEach { character ->
        addView(TextView(this@AresInputMethodService).apply {
          text = if (isShifted) character.uppercase() else character
          textSize = 22f
          gravity = Gravity.CENTER
          setTextColor(Color.BLACK)
          setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
          setOnClickListener {
            currentInputConnection?.commitText(text, 1)
            popup?.dismiss()
          }
        })
      }
    }
    popup = PopupWindow(row, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
      setBackgroundDrawable(GradientDrawable().apply {
        setColor(Color.WHITE)
        cornerRadius = dpToPx(8).toFloat()
        setStroke(1, Color.LTGRAY)
      })
      elevation = dpToPx(8).toFloat()
    }
    popup?.showAsDropDown(anchor, 0, -anchor.height * 2)
  }

  private fun toggleGifPanel() {
    if (showingGifPanel) {
      auxiliaryPanel?.let { contentStack.removeView(it) }
      auxiliaryPanel = null
      showingGifPanel = false
      keysContainer.visibility = View.VISIBLE
      return
    }
    showingGifPanel = true
    keysContainer.visibility = View.GONE
    val panel = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
      layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
    val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    val queryInput = EditText(this).apply {
      hint = "Buscar GIFs"
      singleLine = true
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      layoutParams = LinearLayout.LayoutParams(0, dpToPx(44), 1f)
    }
    val searchButton = createToolButton("Buscar", "Pesquisar GIFs no Tenor") {
      val key = tenorPreferences.getString("api_key", null)
      if (key.isNullOrBlank()) {
        showTenorApiKeyDialog { searchTenorGifs(queryInput.text.toString().trim().ifBlank { "hello" }, it, panel) }
      } else {
        searchTenorGifs(queryInput.text.toString().trim().ifBlank { "hello" }, key, panel)
      }
    }
    controls.addView(queryInput, LinearLayout.LayoutParams(0, dpToPx(44), 1f))
    controls.addView(searchButton)
    panel.addView(controls)
    val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val resultsScroll = ScrollView(this).apply {
      addView(results)
      layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
    }
    panel.addView(resultsScroll)
    val footer = TextView(this).apply {
      text = "Configurar chave Tenor  •  Voltar ao teclado"
      gravity = Gravity.CENTER
      setTextColor(Color.parseColor("#1A73E8"))
      setPadding(0, dpToPx(8), 0, dpToPx(8))
      setOnClickListener {
        AlertDialog.Builder(this@AresInputMethodService)
          .setTitle("Chave da API Tenor")
          .setMessage("A chave é salva apenas nas preferências privadas do ARES neste aparelho.")
          .setPositiveButton("Configurar") { _, _ -> showTenorApiKeyDialog { key -> searchTenorGifs(queryInput.text.toString().trim().ifBlank { "hello" }, key, panel) } }
          .setNegativeButton("Voltar") { _, _ -> toggleGifPanel() }
          .show()
      }
    }
    panel.addView(footer)
    auxiliaryPanel = panel
    contentStack.addView(panel, 0)
  }

  private fun showTenorApiKeyDialog(onSaved: (String) -> Unit) {
    val input = EditText(this).apply {
      hint = "Chave da API Tenor"
      singleLine = true
      setText(tenorPreferences.getString("api_key", "").orEmpty())
    }
    AlertDialog.Builder(this)
      .setTitle("Configurar Tenor")
      .setMessage("Obtenha uma chave no portal de desenvolvedores do Tenor. Não coloque a chave no código-fonte.")
      .setView(input)
      .setPositiveButton("Salvar e pesquisar") { _, _ ->
        val key = input.text.toString().trim()
        if (key.isNotBlank()) {
          tenorPreferences.edit().putString("api_key", key).apply()
          onSaved(key)
        } else {
          Toast.makeText(this, "Informe uma chave válida.", Toast.LENGTH_SHORT).show()
        }
      }
      .setNegativeButton("Cancelar", null)
      .show()
  }

  private fun searchTenorGifs(query: String, apiKey: String, panel: LinearLayout) {
    val results = panel.getChildAt(1) as? ScrollView ?: return
    val resultsList = results.getChildAt(0) as? LinearLayout ?: return
    resultsList.removeAllViews()
    resultsList.addView(TextView(this).apply {
      text = "Buscando GIFs…"
      gravity = Gravity.CENTER
      setPadding(0, dpToPx(12), 0, dpToPx(12))
    })
    serviceScope.launch {
      try {
        val response = withContext(Dispatchers.IO) { tenorApi.buscarGifs(query, apiKey) }
        resultsList.removeAllViews()
        if (response.results.isEmpty()) {
          resultsList.addView(TextView(this@AresInputMethodService).apply {
            text = "Nenhum GIF encontrado. Verifique a chave do Tenor e tente novamente."
            gravity = Gravity.CENTER
          })
          return@launch
        }
        response.results.forEach { gif ->
          val url = gif.mediaFormats?.tinygif?.url ?: gif.mediaFormats?.gif?.url
          if (!url.isNullOrBlank()) {
            val image = ImageView(this@AresInputMethodService).apply {
              adjustViewBounds = true
              scaleType = ImageView.ScaleType.FIT_CENTER
              layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(92)).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(4))
              }
              contentDescription = "GIF do Tenor; toque para inserir no chat"
              setOnClickListener {
                isEnabled = false
                serviceScope.launch {
                  try {
                    val localGif = withContext(Dispatchers.IO) {
                      val gifDir = java.io.File(cacheDir, "gifs")
                      if (!gifDir.exists() && !gifDir.mkdirs()) {
                        throw IllegalStateException("Não foi possível criar o cache de GIFs.")
                      }
                      val gifFile = java.io.File(gifDir, "ares_${System.currentTimeMillis()}.gif")
                      URL(url).openConnection().apply {
                        connectTimeout = 10000
                        readTimeout = 15000
                      }.getInputStream().use { input ->
                        gifFile.outputStream().use { output -> input.copyTo(output) }
                      }
                      gifFile
                    }
                    val inserted = AresMediaInserter(this@AresInputMethodService)
                      .enviarGifParaOApp(localGif, currentInputEditorInfo, url)
                    Toast.makeText(
                      this@AresInputMethodService,
                      if (inserted) "GIF enviado ao campo de mensagem. Revise antes de enviar."
                      else "O app não aceitou GIF direto; o link foi inserido para você revisar.",
                      Toast.LENGTH_LONG
                    ).show()
                  } catch (_: Exception) {
                    currentInputConnection?.commitText(url, 1)
                    Toast.makeText(this@AresInputMethodService, "Não foi possível baixar o GIF; o link foi inserido.", Toast.LENGTH_LONG).show()
                  } finally {
                    isEnabled = true
                  }
                }
              }
            }
            resultsList.addView(image)
            serviceScope.launch(Dispatchers.IO) {
              try {
                val bitmap = URL(url).openConnection().apply {
                  connectTimeout = 8000
                  readTimeout = 8000
                }.getInputStream().use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) withContext(Dispatchers.Main) {
                  if (image.isAttachedToWindow) image.setImageBitmap(bitmap) else bitmap.recycle()
                }
              } catch (_: Exception) { }
            }
          }
        }
      } catch (e: Exception) {
        resultsList.removeAllViews()
        resultsList.addView(TextView(this@AresInputMethodService).apply {
          text = "Falha ao consultar o Tenor. Confira a conexão e a chave da API."
          gravity = Gravity.CENTER
        })
      }
    }
  }

  private fun startVoiceInput() {
    if (!SpeechRecognizer.isRecognitionAvailable(this)) {
      Toast.makeText(this, "Reconhecimento de voz indisponível.", Toast.LENGTH_SHORT).show()
      return
    }
    try {
      speechRecognizer?.destroy()
      speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
        setRecognitionListener(object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) {}
          override fun onBeginningOfSpeech() {}
          override fun onRmsChanged(rmsdB: Float) {}
          override fun onBufferReceived(buffer: ByteArray?) {}
          override fun onEndOfSpeech() {}
          override fun onError(error: Int) {
            Toast.makeText(this@AresInputMethodService, "Falha no reconhecimento de voz: " + error, Toast.LENGTH_SHORT).show()
          }
          override fun onResults(results: Bundle?) {
            val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!spoken.isNullOrBlank()) currentInputConnection?.commitText(spoken, 1)
          }
          override fun onPartialResults(partialResults: Bundle?) {}
          override fun onEvent(eventType: Int, params: Bundle?) {}
        })
      }
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
      }
      speechRecognizer?.startListening(intent)
    } catch (e: Exception) {
      Toast.makeText(this, "Não foi possível iniciar a voz: " + (e.localizedMessage ?: "erro"), Toast.LENGTH_SHORT).show()
    }
  }

  private fun translateBeforeCursor() {
    val ic = currentInputConnection ?: return
    val rawSource = ic.getTextBeforeCursor(2000, 0)?.toString().orEmpty()
    val source = rawSource.trim()
    if (source.isBlank()) {
      Toast.makeText(this, "Digite texto em inglês antes de traduzir.", Toast.LENGTH_SHORT).show()
      return
    }
    val client = translator ?: return
    client.downloadModelIfNeeded()
      .addOnSuccessListener {
        client.translate(source).addOnSuccessListener { translated ->
          val active = currentInputConnection
          if (active != null) {
            active.deleteSurroundingText(rawSource.length, 0)
            active.commitText(translated, 1)
          }
        }.addOnFailureListener {
          Toast.makeText(this, "Falha na tradução.", Toast.LENGTH_SHORT).show()
        }
      }
      .addOnFailureListener {
        Toast.makeText(this, "Não foi possível baixar o modelo de tradução.", Toast.LENGTH_SHORT).show()
      }
  }

  private fun deleteWordsFromCursor(distancePx: Float) {
    val ic = currentInputConnection ?: return
    val before = ic.getTextBeforeCursor(2000, 0)?.toString().orEmpty()
    if (before.isEmpty()) return
    val wordsToDelete = (distancePx / dpToPx(18).coerceAtLeast(1)).toInt().coerceAtLeast(1)
    var index = before.length
    var words = 0
    while (index > 0 && words < wordsToDelete) {
      while (index > 0 && before[index - 1].isWhitespace()) index--
      while (index > 0 && !before[index - 1].isWhitespace()) index--
      words++
    }
    val count = before.length - index
    if (count > 0) ic.deleteSurroundingText(count, 0)
  }

  private fun dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
  }
}
