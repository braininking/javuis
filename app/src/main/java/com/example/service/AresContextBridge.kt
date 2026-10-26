package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cache temporário do contexto da tela.
 *
 * Os consumidores podem ler os StateFlows, mas somente este objeto pode alterá-los.
 * O texto de contexto não é persistido em disco nem incluído em logs.
 */
object AresContextBridge {
  private val allowedPackages = setOf(
    "com.whatsapp",
    "com.instagram.android",
    "org.telegram.messenger",
    "com.facebook.orca",
    "com.google.android.apps.messaging",
    "com.android.mms",
    "com.samsung.android.messaging"
  )

  private val _latestScreenText = MutableStateFlow("")
  private val _detectedAppPackage = MutableStateFlow("")
  private val _pendingTextForKeyboard = MutableStateFlow<String?>(null)

  val latestScreenText: StateFlow<String> = _latestScreenText.asStateFlow()
  val detectedAppPackage: StateFlow<String> = _detectedAppPackage.asStateFlow()
  val pendingTextForKeyboard: StateFlow<String?> = _pendingTextForKeyboard.asStateFlow()

  /**
   * Mantém o contrato usado pelo serviço existente e pelo teclado.
   * Texto vazio ou pacote não autorizado invalida o contexto anterior.
   */
  @Synchronized
  fun updateScreenContext(packageName: String, text: String) {
    if (packageName !in allowedPackages || text.isBlank()) {
      clearContext()
      return
    }

    _detectedAppPackage.value = packageName
    if (_latestScreenText.value != text) {
      _latestScreenText.value = text
    }
  }

  /** Alias em português para chamadas novas, sem remover o contrato original. */
  fun atualizarContexto(packageName: String, novoTexto: String) {
    updateScreenContext(packageName, novoTexto)
  }

  fun queueTextForKeyboard(text: String) {
    _pendingTextForKeyboard.value = text
  }

  /**
   * Preserva a semântica original: consome somente o texto explicitamente enfileirado
   * para o teclado; não consome nem apaga o contexto da tela.
   */
  @Synchronized
  fun consumePendingText(): String? {
    val current = _pendingTextForKeyboard.value
    _pendingTextForKeyboard.value = null
    return current
  }

  @Synchronized
  fun clearContext() {
    _latestScreenText.value = ""
    _detectedAppPackage.value = ""
  }

  fun limparContexto() = clearContext()

  fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val manager = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE)
      as? android.view.accessibility.AccessibilityManager ?: return false
    val enabledServices = manager.getEnabledAccessibilityServiceList(
      android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
    )
    val expectedId = "${context.packageName}/${AresAccessibilityService::class.java.canonicalName}"
    return enabledServices.any { it.id == expectedId || it.id.contains("AresAccessibilityService") }
  }

  fun isKeyboardEnabled(context: android.content.Context): Boolean {
    val manager = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
      as? android.view.inputmethod.InputMethodManager ?: return false
    return manager.enabledInputMethodList.any { it.id.contains("AresInputMethodService") }
  }
}
