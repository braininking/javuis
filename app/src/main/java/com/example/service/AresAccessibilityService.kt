package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AresAccessibilityService : AccessibilityService() {
  private val tag = "AresAccessibility"

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return

    val packageName = event.packageName?.toString().orEmpty()
    // Supported target apps: WhatsApp, Instagram, Telegram, Messages, Messenger, etc.
    if (packageName.contains("whatsapp", ignoreCase = true) ||
      packageName.contains("instagram", ignoreCase = true) ||
      packageName.contains("telegram", ignoreCase = true) ||
      packageName.contains("messaging", ignoreCase = true) ||
      packageName.contains("mms", ignoreCase = true) ||
      packageName == this.packageName
    ) {
      val rootNode = rootInActiveWindow ?: return
      val stringBuilder = StringBuilder()
      extractTextsFromNode(rootNode, stringBuilder, 0)
      val collected = stringBuilder.toString().trim()

      if (collected.isNotBlank()) {
        AresContextBridge.updateScreenContext(packageName, collected)
        Log.d(tag, "Captured ${collected.lines().size} lines of conversation from $packageName")
      }
    }
  }

  private fun extractTextsFromNode(node: AccessibilityNodeInfo?, builder: StringBuilder, depth: Int) {
    if (node == null || depth > 8) return

    val text = node.text?.toString()?.trim()
    val contentDescription = node.contentDescription?.toString()?.trim()

    if (!text.isNullOrBlank() && text.length > 1) {
      builder.append(text).append("\n")
    } else if (!contentDescription.isNullOrBlank() && contentDescription.length > 2) {
      builder.append("[").append(contentDescription).append("]\n")
    }

    for (i in 0 until node.childCount) {
      val child = node.getChild(i)
      extractTextsFromNode(child, builder, depth + 1)
    }
  }

  override fun onInterrupt() {
    Log.d(tag, "AresAccessibilityService interrupted")
  }
}
