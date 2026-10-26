package com.example.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AresContextBridge {
  val latestScreenText = MutableStateFlow("")
  val detectedAppPackage = MutableStateFlow("")
  val pendingTextForKeyboard = MutableStateFlow<String?>(null)

  fun updateScreenContext(packageName: String, text: String) {
    if (text.isNotBlank()) {
      detectedAppPackage.value = packageName
      latestScreenText.value = text
    }
  }

  fun queueTextForKeyboard(text: String) {
    pendingTextForKeyboard.value = text
  }

  fun consumePendingText(): String? {
    val current = pendingTextForKeyboard.value
    pendingTextForKeyboard.value = null
    return current
  }

  fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    val expectedId = "${context.packageName}/${AresAccessibilityService::class.java.canonicalName}"
    return enabledServices.any { it.id == expectedId || it.id.contains("AresAccessibilityService") }
  }

  fun isKeyboardEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager ?: return false
    val list = imm.enabledInputMethodList
    val expected = "${context.packageName}/.keyboard.AresInputMethodService"
    return list.any { it.id.contains("AresInputMethodService") }
  }
}
