package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AresTone
import com.example.ui.theme.AresCardBorder
import com.example.ui.theme.AresCardSurface
import com.example.ui.theme.AresCyanNeon
import com.example.ui.theme.AresKeyAccentBg
import com.example.ui.theme.AresTextMuted
import com.example.ui.theme.AresTextPrimary
import com.example.ui.theme.AresTextSecondary

@Composable
fun AresToneSelector(
  selectedTone: AresTone,
  onToneSelected: (AresTone) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("ares_tone_selector")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "MODO DE TOM DA IA",
        color = AresTextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
      Text(
        text = selectedTone.description,
        color = AresCyanNeon,
        fontSize = 11.sp
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      AresTone.entries.forEach { tone ->
        val isSelected = tone == selectedTone
        val icon = when (tone) {
          AresTone.NATURAL -> Icons.Default.ChatBubbleOutline
          AresTone.ENGRAÇADO -> Icons.Default.Mood
          AresTone.ROMANTICO -> Icons.Default.Favorite
          AresTone.EDUCADO -> Icons.Default.Handshake
          AresTone.DIRETO -> Icons.Default.Bolt
          AresTone.PROFISSIONAL -> Icons.Default.Work
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) AresKeyAccentBg else AresCardSurface)
            .border(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) AresCyanNeon else AresCardBorder,
              shape = RoundedCornerShape(10.dp)
            )
            .clickable { onToneSelected(tone) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("tone_chip_${tone.name.lowercase()}"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = icon,
              contentDescription = tone.displayName,
              tint = if (isSelected) AresCyanNeon else AresTextSecondary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = tone.displayName,
              color = if (isSelected) AresTextPrimary else AresTextSecondary,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          }
        }
      }
    }
  }
}
