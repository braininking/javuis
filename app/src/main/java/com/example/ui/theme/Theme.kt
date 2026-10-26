package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AresColorScheme = darkColorScheme(
  primary = AresCyanNeon,
  onPrimary = AresBgDark,
  primaryContainer = AresKeyAccentBg,
  onPrimaryContainer = AresCyanNeon,
  secondary = AresBlueElectric,
  onSecondary = AresBgDark,
  secondaryContainer = AresCardSurface,
  onSecondaryContainer = AresTextPrimary,
  tertiary = AresVioletCyber,
  background = AresBgDark,
  onBackground = AresTextPrimary,
  surface = AresSurfaceDark,
  onSurface = AresTextPrimary,
  surfaceVariant = AresCardSurface,
  onSurfaceVariant = AresTextSecondary,
  outline = AresCardBorder,
  error = AresErrorRed
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  // Always use the signature ARES dark sci-fi HUD theme
  MaterialTheme(
    colorScheme = AresColorScheme,
    typography = Typography,
    content = content
  )
}
