package com.example

import com.example.data.gemini.AresPersonaEngine
import com.example.data.model.AresTone
import com.example.data.model.ScreenVisionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testAresToneAttributes() {
    assertEquals(6, AresTone.entries.size)
    assertNotNull(AresTone.NATURAL.systemPromptModifier)
    assertNotNull(AresTone.ENGRAÇADO.displayName)
    assertNotNull(AresTone.PROFISSIONAL.displayName)
  }

  @Test
  fun testAresPersonaEngineGreetings() {
    val reply = AresPersonaEngine.generateLocalResponse("Olá ARES, tudo bem?", AresTone.NATURAL)
    assertTrue(reply.contains("Protocolo ARES"))
  }

  @Test
  fun testAresScreenVisionLocalDescribe() {
    val result = AresPersonaEngine.generateScreenVisionLocalResult(
      mode = ScreenVisionMode.DESCRIBE_SCREEN,
      sampleTitle = "WhatsApp",
      visibleText = "Oi, tudo bem?",
      tone = AresTone.NATURAL
    )
    assertEquals(ScreenVisionMode.DESCRIBE_SCREEN, result.mode)
    assertTrue(result.screenSummary.contains("WhatsApp"))
  }

  @Test
  fun testAresScreenVisionLocalReply() {
    val result = AresPersonaEngine.generateScreenVisionLocalResult(
      mode = ScreenVisionMode.REPLY_CONVERSATION,
      sampleTitle = "WhatsApp",
      visibleText = "Você vem hoje?",
      tone = AresTone.DIRETO
    )
    assertEquals(ScreenVisionMode.REPLY_CONVERSATION, result.mode)
    assertTrue(result.suggestedReply.isNotBlank())
  }
}
