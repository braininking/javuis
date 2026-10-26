package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.gemini.GeminiRepository
import com.example.data.model.AssistantHudState
import com.example.ui.components.AresHudHeader
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.KeyboardSetupScreen
import com.example.ui.screens.SmartVisionScreen
import com.example.ui.screens.SystemStatusScreen
import com.example.ui.theme.AresBgDark
import com.example.ui.theme.AresCardBorder
import com.example.ui.theme.AresCyanGlow
import com.example.ui.theme.AresCyanNeon
import com.example.ui.theme.AresKeyAccentBg
import com.example.ui.theme.AresSurfaceDark
import com.example.ui.theme.AresTextMuted
import com.example.ui.theme.AresTextPrimary
import com.example.ui.theme.AresTextSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.voice.AresVoiceManager

enum class AresScreen(val title: String) {
  CHAT("Chat ARES"),
  VISION("Visão"),
  KEYBOARD("Teclado"),
  STATUS("Status")
}

class MainActivity : ComponentActivity() {
  private lateinit var geminiRepository: GeminiRepository
  private lateinit var voiceManager: AresVoiceManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    geminiRepository = GeminiRepository(this)
    voiceManager = AresVoiceManager(this)

    setContent {
      MyApplicationTheme {
        AresMainApp(
          geminiRepository = geminiRepository,
          voiceManager = voiceManager
        )
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    voiceManager.stopListening()
  }
}

@Composable
fun AresMainApp(
  geminiRepository: GeminiRepository,
  voiceManager: AresVoiceManager
) {
  var currentScreen by remember { mutableStateOf(AresScreen.CHAT) }
  val isCloudReady = remember { geminiRepository.isCloudKeyConfigured() }

  // Mandated BackHandler for custom state navigation
  if (currentScreen != AresScreen.CHAT) {
    BackHandler {
      currentScreen = AresScreen.CHAT
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      AresHudHeader(
        hudState = AssistantHudState.STANDBY,
        isCloudReady = isCloudReady
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = AresSurfaceDark,
        tonalElevation = 8.dp,
        modifier = Modifier
          .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
          .border(
            width = 1.dp,
            color = AresCardBorder,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
          )
          .testTag("ares_navigation_bar")
      ) {
        NavigationBarItem(
          selected = currentScreen == AresScreen.CHAT,
          onClick = { currentScreen = AresScreen.CHAT },
          icon = {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Chat,
              contentDescription = "Chat com IA",
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = "Chat IA",
              fontSize = 10.sp,
              fontWeight = if (currentScreen == AresScreen.CHAT) FontWeight.Bold else FontWeight.Normal,
              fontFamily = FontFamily.Monospace
            )
          },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = AresCyanNeon,
            selectedTextColor = AresCyanNeon,
            unselectedIconColor = AresTextMuted,
            unselectedTextColor = AresTextMuted,
            indicatorColor = AresKeyAccentBg
          ),
          modifier = Modifier.testTag("nav_item_chat")
        )

        NavigationBarItem(
          selected = currentScreen == AresScreen.VISION,
          onClick = { currentScreen = AresScreen.VISION },
          icon = {
            Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = "Visão Inteligente",
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = "Visão",
              fontSize = 10.sp,
              fontWeight = if (currentScreen == AresScreen.VISION) FontWeight.Bold else FontWeight.Normal,
              fontFamily = FontFamily.Monospace
            )
          },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = AresCyanNeon,
            selectedTextColor = AresCyanNeon,
            unselectedIconColor = AresTextMuted,
            unselectedTextColor = AresTextMuted,
            indicatorColor = AresKeyAccentBg
          ),
          modifier = Modifier.testTag("nav_item_vision")
        )

        NavigationBarItem(
          selected = currentScreen == AresScreen.KEYBOARD,
          onClick = { currentScreen = AresScreen.KEYBOARD },
          icon = {
            Icon(
              imageVector = Icons.Default.Keyboard,
              contentDescription = "Teclado ARES",
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = "Teclado",
              fontSize = 10.sp,
              fontWeight = if (currentScreen == AresScreen.KEYBOARD) FontWeight.Bold else FontWeight.Normal,
              fontFamily = FontFamily.Monospace
            )
          },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = AresCyanNeon,
            selectedTextColor = AresCyanNeon,
            unselectedIconColor = AresTextMuted,
            unselectedTextColor = AresTextMuted,
            indicatorColor = AresKeyAccentBg
          ),
          modifier = Modifier.testTag("nav_item_keyboard")
        )

        NavigationBarItem(
          selected = currentScreen == AresScreen.STATUS,
          onClick = { currentScreen = AresScreen.STATUS },
          icon = {
            Icon(
              imageVector = Icons.Default.Terminal,
              contentDescription = "Status e Diagnóstico",
              modifier = Modifier.size(22.dp)
            )
          },
          label = {
            Text(
              text = "Status",
              fontSize = 10.sp,
              fontWeight = if (currentScreen == AresScreen.STATUS) FontWeight.Bold else FontWeight.Normal,
              fontFamily = FontFamily.Monospace
            )
          },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = AresCyanNeon,
            selectedTextColor = AresCyanNeon,
            unselectedIconColor = AresTextMuted,
            unselectedTextColor = AresTextMuted,
            indicatorColor = AresKeyAccentBg
          ),
          modifier = Modifier.testTag("nav_item_status")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(AresBgDark)
        .padding(innerPadding)
    ) {
      when (currentScreen) {
        AresScreen.CHAT -> {
          ChatScreen(
            geminiRepository = geminiRepository,
            voiceManager = voiceManager,
            onNavigateToVision = { currentScreen = AresScreen.VISION },
            onNavigateToKeyboard = { currentScreen = AresScreen.KEYBOARD }
          )
        }
        AresScreen.VISION -> {
          SmartVisionScreen(
            geminiRepository = geminiRepository,
            onNavigateToKeyboard = { currentScreen = AresScreen.KEYBOARD }
          )
        }
        AresScreen.KEYBOARD -> {
          KeyboardSetupScreen()
        }
        AresScreen.STATUS -> {
          SystemStatusScreen(
            geminiRepository = geminiRepository
          )
        }
      }
    }
  }
}
