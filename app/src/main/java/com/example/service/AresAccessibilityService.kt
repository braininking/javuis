package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AresAccessibilityService : AccessibilityService() {

  private var ultimoTimestampCaptura: Long = 0L
  private var ultimoTextoConsolidado: String = ""
  private var ultimoPacoteAlvo: String = ""

  private val appsPermitidos = setOf(
    "com.whatsapp",
    "com.instagram.android",
    "org.telegram.messenger",
    "com.facebook.orca",
    "com.google.android.apps.messaging",
    "com.android.mms",
    "com.samsung.android.messaging"
  )

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return
    val packageName = event.packageName?.toString() ?: run {
      limparEstadoLocal()
      AresContextBridge.clearContext()
      return
    }

    // Eventos da própria interface do ARES não devem substituir o contexto do chat.
    // Ao abrir uma tela do próprio app, invalida-se o contexto anterior.
    if (packageName == this.packageName) {
      if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
        limparEstadoLocal()
        AresContextBridge.clearContext()
      }
      return
    }

    // Qualquer aplicativo fora da lista autorizada invalida imediatamente o cache.
    if (packageName !in appsPermitidos) {
      limparEstadoLocal()
      AresContextBridge.clearContext()
      return
    }

    // Ao trocar entre aplicativos permitidos, nunca reutiliza o contexto do app anterior.
    if (packageName != ultimoPacoteAlvo) {
      AresContextBridge.clearContext()
      ultimoTextoConsolidado = ""
      ultimoTimestampCaptura = 0L
      ultimoPacoteAlvo = packageName
    }

    val tempoAtual = SystemClock.uptimeMillis()
    if (tempoAtual - ultimoTimestampCaptura < 400L) return

    val rootNode = rootInActiveWindow
    if (rootNode == null) {
      limparEstadoLocal()
      AresContextBridge.clearContext()
      return
    }

    try {
      val builder = StringBuilder()
      extrairTextosRecursivo(rootNode, builder, 0)
      val textoCapturado = builder.toString().trim().take(MAX_CONTEXT_CHARS)

      // Atualiza o timestamp mesmo quando o texto não mudou, evitando varreduras repetidas.
      ultimoTimestampCaptura = tempoAtual

      if (textoCapturado != ultimoTextoConsolidado) {
        ultimoTextoConsolidado = textoCapturado
        if (textoCapturado.isBlank()) {
          AresContextBridge.clearContext()
        } else {
          AresContextBridge.updateScreenContext(packageName, textoCapturado)
        }
      }
    } finally {
      // O serviço é responsável somente pelo nó raiz que obteve.
      rootNode.recycle()
    }
  }

  private fun extrairTextosRecursivo(
    node: AccessibilityNodeInfo?,
    builder: StringBuilder,
    depth: Int
  ) {
    if (node == null || depth > MAX_DEPTH || builder.length >= MAX_CONTEXT_CHARS) return

    // Não lê o conteúdo nem os descendentes de campos marcados como senha.
    if (node.isPassword) return

    val text = node.text?.toString()?.trim()
    val description = node.contentDescription?.toString()?.trim()

    when {
      !text.isNullOrBlank() -> builder.append(text).append('\n')
      !description.isNullOrBlank() && description.length > 2 ->
        builder.append('[').append(description).append(']').append('\n')
    }

    if (builder.length >= MAX_CONTEXT_CHARS) return

    for (i in 0 until node.childCount) {
      if (builder.length >= MAX_CONTEXT_CHARS) break
      val child = node.getChild(i) ?: continue
      try {
        extrairTextosRecursivo(child, builder, depth + 1)
      } finally {
        // Cada nó filho é liberado exclusivamente por quem chamou getChild().
        child.recycle()
      }
    }
  }

  private fun limparEstadoLocal() {
    ultimoTextoConsolidado = ""
    ultimoTimestampCaptura = 0L
    ultimoPacoteAlvo = ""
  }

  override fun onInterrupt() {
    limparEstadoLocal()
    AresContextBridge.clearContext()
  }

  companion object {
    private const val MAX_DEPTH = 8
    private const val MAX_CONTEXT_CHARS = 6000
  }
}
