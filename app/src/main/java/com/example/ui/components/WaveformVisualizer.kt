package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonRose
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    rmsLevel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WavePhaseTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val secondaryPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "secondaryPhase"
    )

    // Smoothly interpolate the RMS amplitude to avoid jerky jumps
    val targetAmplitude = if (isListening) {
        val base = 6f
        val boost = rmsLevel * 32f
        base + boost
    } else {
        2f
    }

    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "amplitude"
    )

    val waveGradient = Brush.horizontalGradient(
        colors = listOf(
            NeonEmerald,
            NeonCyan,
            NeonIndigo,
            NeonRose
        )
    )

    val secondaryGradient = Brush.horizontalGradient(
        colors = listOf(
            NeonRose.copy(alpha = 0.5f),
            NeonIndigo.copy(alpha = 0.6f),
            NeonEmerald.copy(alpha = 0.4f)
        )
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        if (width <= 0f || height <= 0f) return@Canvas

        // 1. Draw glowing secondary harmonic wave in background
        val secondaryPath = Path()
        val step = 4f
        var x = 0f
        var first = true

        while (x <= width) {
            val progress = x / width
            // Bell curve windowing so wave tapers neatly at left & right borders
            val window = sin(progress * PI.toFloat()).coerceAtLeast(0f)
            val y = centerY + sin(progress * 4f * PI.toFloat() + secondaryPhase) * (animatedAmplitude * 0.65f) * window

            if (first) {
                secondaryPath.moveTo(x, y)
                first = false
            } else {
                secondaryPath.lineTo(x, y)
            }
            x += step
        }

        // Draw soft glow behind
        drawPath(
            path = secondaryPath,
            brush = secondaryGradient,
            style = Stroke(
                width = 6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 2. Draw primary energetic wave
        val primaryPath = Path()
        x = 0f
        first = true

        while (x <= width) {
            val progress = x / width
            val window = sin(progress * PI.toFloat()).coerceAtLeast(0f)
            val y = centerY + sin(progress * 3f * PI.toFloat() + phase) * animatedAmplitude * window

            if (first) {
                primaryPath.moveTo(x, y)
                first = false
            } else {
                primaryPath.lineTo(x, y)
            }
            x += step
        }

        // Glow pass
        drawPath(
            path = primaryPath,
            brush = waveGradient,
            alpha = 0.45f,
            style = Stroke(
                width = 8.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Core sharp line pass
        drawPath(
            path = primaryPath,
            brush = waveGradient,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 3. Center pulsating dot when actively hearing speech
        if (rmsLevel > 0.15f) {
            val centerDotRadius = (2.dp.toPx() + rmsLevel * 5.dp.toPx())
            drawCircle(
                brush = waveGradient,
                radius = centerDotRadius,
                center = Offset(width / 2f, centerY)
            )
        }
    }
}
