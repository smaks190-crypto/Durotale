package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonRose

/**
 * High-aesthetic moving neon glow stripe visualizer (height = 10.dp).
 * Features an infinite multi-color gradient (Emerald -> Indigo -> Cyan -> Rose)
 * with traveling neon light pulses that dynamically expand with voice loudness (voiceLevel).
 */
@Composable
fun MovingNeonGlowVisualizer(
    voiceLevel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(10.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NeonFlowTransition")

    // Smooth continuous translation phase 0f -> 1f
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Secondary reverse shimmer phase
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )

    // Smooth reaction to microphone audio amplitude
    val targetAmplitude = if (isListening) voiceLevel.coerceIn(0f, 1f) else 0f
    val animatedVoice by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "voiceAmplitude"
    )

    val neonColors = listOf(
        NeonEmerald,
        NeonIndigo,
        NeonCyan,
        NeonRose,
        NeonEmerald,
        NeonCyan
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val corner = CornerRadius(height / 2f, height / 2f)

        // 1. Dark base track with subtle neon edge
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = corner
        )

        // 2. Underlying ambient moving gradient
        val ambientBrush = Brush.linearGradient(
            colors = neonColors,
            start = Offset(phase * width - width, 0f),
            end = Offset(phase * width + width, 0f)
        )

        val baseAlpha = if (isListening) 0.35f + animatedVoice * 0.35f else 0.15f
        drawRoundRect(
            brush = ambientBrush,
            alpha = baseAlpha,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = corner
        )

        // 3. Primary moving light beam spot
        // Travels from left to right and dynamically widens with voice energy
        val beamWidth = (width * 0.28f) + (animatedVoice * width * 0.55f)
        val beamCenter = (phase * (width + beamWidth * 1.5f)) - (beamWidth * 0.75f)
        val beamLeft = (beamCenter - beamWidth / 2f).coerceIn(0f, width)
        val beamRight = (beamCenter + beamWidth / 2f).coerceIn(0f, width)
        val actualBeamWidth = (beamRight - beamLeft).coerceAtLeast(0f)

        if (actualBeamWidth > 0f) {
            val beamBrush = Brush.horizontalGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0f),
                    NeonEmerald.copy(alpha = 0.85f + animatedVoice * 0.15f),
                    NeonCyan.copy(alpha = 1f),
                    NeonRose.copy(alpha = 0.85f),
                    NeonIndigo.copy(alpha = 0f)
                ),
                startX = beamLeft,
                endX = beamRight
            )

            // Core beam pass
            drawRoundRect(
                brush = beamBrush,
                topLeft = Offset(beamLeft, 0f),
                size = Size(actualBeamWidth, height),
                cornerRadius = corner
            )
        }

        // 4. Secondary gentle reverse counter-beam
        val counterBeamWidth = width * 0.2f
        val counterCenter = (shimmerPhase * (width + counterBeamWidth * 2f)) - counterBeamWidth
        val counterLeft = (counterCenter - counterBeamWidth / 2f).coerceIn(0f, width)
        val counterRight = (counterCenter + counterBeamWidth / 2f).coerceIn(0f, width)
        val actualCounterWidth = (counterRight - counterLeft).coerceAtLeast(0f)

        if (actualCounterWidth > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        NeonRose.copy(alpha = 0.4f),
                        NeonCyan.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    startX = counterLeft,
                    endX = counterRight
                ),
                topLeft = Offset(counterLeft, 1f),
                size = Size(actualCounterWidth, height - 2f),
                cornerRadius = corner
            )
        }

        // 5. Crisp glowing outline
        drawRoundRect(
            brush = ambientBrush,
            alpha = if (isListening) 0.85f else 0.4f,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = corner,
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}
