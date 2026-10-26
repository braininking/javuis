package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.service.AresContextBridge
import com.example.ui.theme.AresBgDark
import com.example.ui.theme.AresCardBorder
import com.example.ui.theme.AresCardSurface
import com.example.ui.theme.AresCyanGlow
import com.example.ui.theme.AresCyanNeon
import com.example.ui.theme.AresSurfaceDark
import com.example.ui.theme.AresTextMuted
import com.example.ui.theme.AresTextPrimary
import com.example.ui.theme.AresTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AresMessageItem(
  message: ChatMessage,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isUser = message.sender == MessageSender.USER
  val isAres = message.sender == MessageSender.ARES

  val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
  val timeStr = timeFormat.format(Date(message.timestamp))

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .testTag("chat_message_${message.id}"),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    if (isAres) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(AresSurfaceDark)
          .border(1.dp, AresCyanNeon, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Bolt,
          contentDescription = "ARES Avatar",
          tint = AresCyanNeon,
          modifier = Modifier.size(16.dp)
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
    }

    Column(
      modifier = Modifier
        .fillMaxWidth(if (isUser) 0.85f else 0.88f)
        .clip(
          RoundedCornerShape(
            topStart = 14.dp,
            topEnd = 14.dp,
            bottomStart = if (isUser) 14.dp else 2.dp,
            bottomEnd = if (isUser) 2.dp else 14.dp
          )
        )
        .background(if (isUser) Color(0xFF0F263E) else AresCardSurface)
        .border(
          width = 1.dp,
          color = if (isUser) AresCyanGlow.copy(alpha = 0.5f) else AresCardBorder,
          shape = RoundedCornerShape(
            topStart = 14.dp,
            topEnd = 14.dp,
            bottomStart = if (isUser) 14.dp else 2.dp,
            bottomEnd = if (isUser) 2.dp else 14.dp
          )
        )
        .padding(12.dp)
    ) {
      // Header tag
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (isUser) "VOCÊ" else "ARES",
            color = if (isUser) AresCyanNeon else AresCyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
          )

          if (message.isVoiceInput) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.Mic,
              contentDescription = "Comando de voz",
              tint = AresCyanNeon,
              modifier = Modifier.size(12.dp)
            )
          }

          if (message.tone != null && isAres) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(AresSurfaceDark)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = message.tone.displayName,
                color = AresTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        Text(
          text = timeStr,
          color = AresTextMuted,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = message.content,
        color = AresTextPrimary,
        fontSize = 14.sp,
        lineHeight = 20.sp
      )

      if (isAres) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Send to Keyboard Action
          IconButton(
            onClick = {
              AresContextBridge.queueTextForKeyboard(message.content)
              Toast.makeText(context, "Enviado para o Teclado ARES! Pronto para inserção.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(32.dp).testTag("insert_to_keyboard_${message.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Keyboard,
              contentDescription = "Enviar resposta para o Teclado ARES",
              tint = AresCyanNeon,
              modifier = Modifier.size(16.dp)
            )
          }

          Spacer(modifier = Modifier.width(4.dp))

          // Copy Action
          IconButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("ARES Resposta", message.content)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "Texto copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(32.dp).testTag("copy_message_${message.id}")
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copiar resposta da IA",
              tint = AresTextSecondary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    if (isUser) {
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(AresCardSurface)
          .border(1.dp, AresCardBorder, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = "User Avatar",
          tint = AresTextSecondary,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
