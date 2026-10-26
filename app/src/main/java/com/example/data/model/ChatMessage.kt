package com.example.data.model

data class ChatMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val sender: MessageSender,
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
  val tone: AresTone? = null,
  val isVoiceInput: Boolean = false,
  val attachedImageUri: String? = null,
  val isFromScreenVision: Boolean = false,
  val isCopiedOrInserted: Boolean = false
)

enum class MessageSender {
  USER,
  ARES,
  SYSTEM
}

enum class AssistantHudState {
  STANDBY,
  LISTENING_VOICE,
  PROCESSING,
  SPEAKING,
  ANALYZING_SCREEN,
  ERROR
}

data class ScreenAnalysisResult(
  val mode: ScreenVisionMode,
  val detectedApp: String = "Conversa Detectada",
  val detectedSpeaker: String = "Interlocutor",
  val visibleMessages: List<String> = emptyList(),
  val screenSummary: String = "",
  val suggestedReply: String = "",
  val selectedTone: AresTone = AresTone.NATURAL,
  val confidenceScore: Float = 0.94f,
  val isMockSample: Boolean = false
)

enum class ScreenVisionMode {
  DESCRIBE_SCREEN,
  REPLY_CONVERSATION
}
