package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantHudState
import com.example.ui.theme.AresBlueElectric
import com.example.ui.theme.AresCyanGlow
import com.example.ui.theme.AresCyanNeon
import com.example.ui.theme.AresErrorRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AresReactorCore(
  hudState: AssistantHudState,
  audioRms: Float = 0f,
  onCoreClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "reactor_anim")

  val rotationClockwise by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(8000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation_cw"
  )

  val rotationCounterClockwise by infiniteTransition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(5000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation_ccw"
  )

  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  val coreColor = when (hudState) {
    AssistantHudState.ERROR -> AresErrorRed
    AssistantHudState.LISTENING_VOICE -> AresCyanNeon
    AssistantHudState.PROCESSING -> AresBlueElectric
    else -> AresCyanNeon
  }

  Box(
    modifier = modifier
      .size(140.dp)
      .testTag("ares_reactor_core")
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = false, radius = 70.dp),
        onClick = onCoreClick
      ),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = (size.minDimension / 2f) * 0.85f

      // Outer Glow circle
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(coreColor.copy(alpha = 0.35f), Color.Transparent),
          center = center,
          radius = radius * 1.3f
        ),
        radius = radius * 1.25f,
        center = center
      )

      // Base Track Circle
      drawCircle(
        color = AresCyanGlow.copy(alpha = 0.15f),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )

      // Outer Rotating Arc Segments (Clockwise)
      rotate(rotationClockwise, pivot = center) {
        val sweepAngle = 55f
        for (i in 0 until 4) {
          val startAngle = i * 90f + 10f
          drawArc(
            color = coreColor.copy(alpha = 0.8f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
          )
        }
      }

      // Middle Track & Counter-rotating Arcs
      val midRadius = radius * 0.72f
      rotate(rotationCounterClockwise, pivot = center) {
        for (i in 0 until 6) {
          val startAngle = i * 60f + 5f
          drawArc(
            color = AresBlueElectric.copy(alpha = 0.6f),
            startAngle = startAngle,
            sweepAngle = 25f,
            useCenter = false,
            topLeft = Offset(center.x - midRadius, center.y - midRadius),
            size = androidx.compose.ui.geometry.Size(midRadius * 2, midRadius * 2),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
          )
        }
      }

      // Center Core Orb with voice reactivity
      val audioFactor = (audioRms.coerceIn(0f, 15f) / 15f) * 0.3f
      val dynamicInnerRadius = (radius * 0.38f) * (pulseScale + audioFactor)

      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.White,
            coreColor,
            coreColor.copy(alpha = 0.2f),
            Color.Transparent
          ),
          center = center,
          radius = dynamicInnerRadius * 1.4f
        ),
        radius = dynamicInnerRadius,
        center = center
      )

      // Radial HUD ticks
      val tickCount = 12
      for (i in 0 until tickCount) {
        val angle = Math.toRadians((i * (360.0 / tickCount)))
        val startX = center.x + (radius * 0.84f) * cos(angle).toFloat()
        val startY = center.y + (radius * 0.84f) * sin(angle).toFloat()
        val endX = center.x + (radius * 0.94f) * cos(angle).toFloat()
        val endY = center.y + (radius * 0.94f) * sin(angle).toFloat()

        drawLine(
          color = coreColor.copy(alpha = 0.5f),
          start = Offset(startX, startY),
          end = Offset(endX, endY),
          strokeWidth = 1.5.dp.toPx(),
          cap = StrokeCap.Round
        )
      }
    }
  }
}
