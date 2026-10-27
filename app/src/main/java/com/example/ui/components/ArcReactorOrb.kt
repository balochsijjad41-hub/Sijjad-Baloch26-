package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.ui.theme.OrbitronFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorOrb(
    state: AssistantState,
    audioLevel: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    glowColor: Color = MaterialTheme.colorScheme.secondary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb")

    // Continuous rotation for outer rings
    val rotationAngleCW by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_cw"
    )

    val rotationAngleCCW by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_ccw"
    )

    // Breathing pulse for idle/standby
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Dynamic scale driven by audio volume when speaking or listening
    val activeScale = when (state) {
        AssistantState.LISTENING -> 1.0f + (audioLevel * 0.45f)
        AssistantState.SPEAKING -> 1.0f + (breathScale * 0.15f) + (audioLevel * 0.25f)
        AssistantState.THINKING -> breathScale * 1.1f
        AssistantState.STANDBY -> breathScale
        AssistantState.ERROR -> 0.95f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2f) * 0.85f

                // 1. Ambient Glow Field
                val glowRadius = baseRadius * activeScale * 1.15f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (state == AssistantState.LISTENING) 0.35f else 0.22f),
                            glowColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = center
                )

                // 2. Outer Segmented Ring (Rotates Clockwise)
                rotate(rotationAngleCW, center) {
                    drawSegmentedRing(center, baseRadius * 0.96f, accentColor, glowColor)
                }

                // 3. Counter-Rotating Middle Tech Arc Ring
                rotate(rotationAngleCCW, center) {
                    drawMiddleArcs(center, baseRadius * 0.76f, accentColor)
                }

                // 4. Inner Ring with Tick Markers
                rotate(rotationAngleCW * 0.5f, center) {
                    drawTickRing(center, baseRadius * 0.58f, accentColor.copy(alpha = 0.6f))
                }

                // 5. Core Arc Reactor Central Sphere
                val coreRadius = baseRadius * 0.38f * activeScale
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            accentColor,
                            glowColor.copy(alpha = 0.8f),
                            accentColor.copy(alpha = 0.2f)
                        ),
                        center = center,
                        radius = coreRadius
                    ),
                    radius = coreRadius,
                    center = center
                )

                // Inner core ring border
                drawCircle(
                    color = Color.White,
                    radius = coreRadius * 0.8f,
                    center = center,
                    style = Stroke(width = 2.5f)
                )

                // 6. Holographic Crosshair Reticle in Thinking/Active Mode
                if (state == AssistantState.THINKING || state == AssistantState.LISTENING) {
                    drawReticle(center, baseRadius * 0.45f, accentColor.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Futuristic Status Label with HUD brackets (Roman Urdu & English)
        val (stateText, stateColor) = when (state) {
            AssistantState.LISTENING -> "[ SUN RAHA HOON // LISTENING ]" to accentColor
            AssistantState.THINKING -> "[ SOCH RAHA HOON // PROCESSING ]" to glowColor
            AssistantState.SPEAKING -> "[ BOL RAHA HOON // SPEAKING ]" to accentColor
            AssistantState.STANDBY -> "[ JARVIS CORE // FA'AAL ]" to MaterialTheme.colorScheme.onSurfaceVariant
            AssistantState.ERROR -> "[ ITTILA // ADVISORY ]" to Color(0xFFFF5252)
        }

        Text(
            text = stateText,
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
            color = stateColor
        )
    }
}

private fun DrawScope.drawSegmentedRing(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val strokeWidth = 3f
    val sweepAngle = 50f
    val gapAngle = 10f
    val numSegments = 6

    for (i in 0 until numSegments) {
        val startAngle = i * (sweepAngle + gapAngle)
        drawArc(
            color = if (i % 2 == 0) primaryColor else secondaryColor.copy(alpha = 0.7f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawMiddleArcs(
    center: Offset,
    radius: Float,
    color: Color
) {
    val strokeWidth = 4f
    val arcLen = 70f
    val gaps = 50f

    for (i in 0 until 3) {
        val startAngle = i * (arcLen + gaps)
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = arcLen,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth)
        )
    }
}

private fun DrawScope.drawTickRing(
    center: Offset,
    radius: Float,
    color: Color
) {
    val numTicks = 24
    for (i in 0 until numTicks) {
        val angleRad = (i * (360f / numTicks)) * (PI / 180f)
        val innerR = radius - 6f
        val outerR = radius + 6f
        val start = Offset(
            x = center.x + (innerR * cos(angleRad)).toFloat(),
            y = center.y + (innerR * sin(angleRad)).toFloat()
        )
        val end = Offset(
            x = center.x + (outerR * cos(angleRad)).toFloat(),
            y = center.y + (outerR * sin(angleRad)).toFloat()
        )
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawReticle(center: Offset, len: Float, color: Color) {
    val gap = len * 0.35f
    // Top
    drawLine(color, Offset(center.x, center.y - len), Offset(center.x, center.y - gap), strokeWidth = 1.8f)
    // Bottom
    drawLine(color, Offset(center.x, center.y + gap), Offset(center.x, center.y + len), strokeWidth = 1.8f)
    // Left
    drawLine(color, Offset(center.x - len, center.y), Offset(center.x - gap, center.y), strokeWidth = 1.8f)
    // Right
    drawLine(color, Offset(center.x + gap, center.y), Offset(center.x + len, center.y), strokeWidth = 1.8f)
}
