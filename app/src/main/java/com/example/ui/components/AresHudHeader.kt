package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantHudState
import com.example.ui.theme.AresBgDark
import com.example.ui.theme.AresCardBorder
import com.example.ui.theme.AresCyanNeon
import com.example.ui.theme.AresSuccessGreen
import com.example.ui.theme.AresSurfaceDark
import com.example.ui.theme.AresTextMuted
import com.example.ui.theme.AresTextPrimary
import com.example.ui.theme.AresTextSecondary
import com.example.ui.theme.AresWarningAmber

@Composable
fun AresHudHeader(
  hudState: AssistantHudState,
  isCloudReady: Boolean,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(AresBgDark)
      .padding(horizontal = 16.dp, vertical = 10.dp)
      .testTag("ares_hud_header")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Logo & Model name
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(AresSurfaceDark)
            .border(1.5.dp, AresCyanNeon, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = "ARES Reactor Icon",
            tint = AresCyanNeon,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "ARES",
              color = AresCyanNeon,
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "v1.0 • JARVIS CORE",
              color = AresTextMuted,
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium,
              fontFamily = FontFamily.Monospace
            )
          }

          Text(
            text = when (hudState) {
              AssistantHudState.STANDBY -> "SISTEMA ONLINE // PRONTO"
              AssistantHudState.LISTENING_VOICE -> "ESCUTANDO COMANDO DE VOZ..."
              AssistantHudState.PROCESSING -> "PROCESSANDO RESPOSTA NEURAL..."
              AssistantHudState.SPEAKING -> "TRANSMITINDO ÁUDIO..."
              AssistantHudState.ANALYZING_SCREEN -> "ANALISANDO VISÃO DA TELA..."
              AssistantHudState.ERROR -> "ALERTA NO PROTOCOLO"
            },
            color = if (hudState == AssistantHudState.LISTENING_VOICE || hudState == AssistantHudState.PROCESSING)
              AresCyanNeon else AresTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Right: Telemetry & Engine Badge
      Column(horizontalAlignment = Alignment.End) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCloudReady) AresSurfaceDark else Color(0xFF1E1710))
            .border(
              width = 1.dp,
              color = if (isCloudReady) AresSuccessGreen.copy(alpha = pulseAlpha) else AresWarningAmber,
              shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isCloudReady) AresSuccessGreen else AresWarningAmber)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = if (isCloudReady) "GEMINI 3.8 FLASH" else "NÚCLEO LOCAL",
              color = if (isCloudReady) AresTextPrimary else AresWarningAmber,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        Spacer(modifier = Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "Device profile",
            tint = AresTextMuted,
            modifier = Modifier.size(11.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "Galaxy A36/A72 Opt",
            color = AresTextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    // Cyber line separator
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(AresCardBorder)
    )
  }
}
