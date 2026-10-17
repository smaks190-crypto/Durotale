package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.model.BattlePhase
import com.example.model.BattleState
import com.example.ui.theme.RetroBoxBlack
import com.example.ui.theme.RetroGreen
import com.example.ui.theme.RetroOrange
import com.example.ui.theme.RetroRed
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow

@Composable
fun UndertaleBattleView(
    battle: BattleState,
    tick: Long,
    onSoulPositionChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .background(RetroBoxBlack, RoundedCornerShape(2.dp))
            .border(3.dp, RetroWhite, RoundedCornerShape(2.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val scaleX = 640f / size.width
                    val scaleY = 400f / size.height
                    val newX = battle.soulX + dragAmount.x * scaleX
                    val newY = battle.soulY + dragAmount.y * scaleY
                    onSoulPositionChanged(newX, newY)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scaleX = size.width / 640f
            val scaleY = size.height / 400f

            fun rect(x: Float, y: Float, w: Float, h: Float, color: Color) {
                drawRect(
                    color = color,
                    topLeft = Offset(x * scaleX, y * scaleY),
                    size = Size(w * scaleX, h * scaleY)
                )
            }

            fun strokeRect(x: Float, y: Float, w: Float, h: Float, color: Color, strokeWidth: Float = 2f) {
                drawRect(
                    color = color,
                    topLeft = Offset(x * scaleX, y * scaleY),
                    size = Size(w * scaleX, h * scaleY),
                    style = Stroke(width = strokeWidth * scaleX)
                )
            }

            // Undertale black background with green grid lines
            drawRect(color = Color.Black)
            for (gx in 40 until 600 step 40) {
                strokeRect(gx.toFloat(), 15f, 40f, 150f, Color(0xFF0D2614), 1f)
            }

            // ENEMY: Санитар Михалыч (Undertale Monster Boss style)
            val shakeX = if (battle.enemyShake > 0) ((Math.random() - 0.5) * 14.0).toFloat() else 0f
            val ex = 275f + shakeX
            val ey = 25f

            // Enemy HP Bar
            rect(250f, 12f, 140f, 10f, Color(0xFF444444))
            val hpRatio = (battle.enemyHP.toFloat() / battle.enemyMaxHP.toFloat()).coerceIn(0f, 1f)
            rect(250f, 12f, 140f * hpRatio, 10f, RetroGreen)
            strokeRect(250f, 12f, 140f, 10f, RetroWhite, 2f)

            // Boss Sprite: Михалыч
            // Cap
            rect(ex + 18f, ey + 6f, 54f, 16f, Color.White)
            rect(ex + 41f, ey + 9f, 8f, 10f, Color(0xFFFF3D00))
            rect(ex + 38f, ey + 12f, 14f, 4f, Color(0xFFFF3D00))

            // Head
            rect(ex + 14f, ey + 24f, 62f, 42f, Color.White)
            // Angry eyebrows & eyes
            rect(ex + 24f, ey + 34f, 14f, 5f, Color.Black)
            rect(ex + 52f, ey + 34f, 14f, 5f, Color.Black)
            rect(ex + 28f, ey + 40f, 6f, 6f, Color.Black)
            rect(ex + 56f, ey + 40f, 6f, 6f, Color.Black)
            // Mustache
            rect(ex + 26f, ey + 52f, 38f, 7f, Color.Black)

            // Torso White Coat
            rect(ex + 4f, ey + 68f, 82f, 68f, Color.White)
            rect(ex + 42f, ey + 68f, 6f, 68f, Color.Black)
            // Giant Syringe in right hand
            rect(ex + 90f, ey + 52f, 12f, 36f, Color(0xFF76FF03))
            rect(ex + 94f, ey + 36f, 4f, 16f, Color.White)

            // Slash animation & damage indicator
            if (battle.phase == BattlePhase.SLASH_ANIM) {
                drawLine(
                    color = Color(0xFFFF1744),
                    start = Offset((ex + 10f) * scaleX, (ey + 15f) * scaleY),
                    end = Offset((ex + 80f) * scaleX, (ey + 125f) * scaleY),
                    strokeWidth = 5f * scaleX
                )
                // Red damage star
                rect(ex + 30f, ey + 50f, 25f, 12f, RetroYellow)
            }

            // Undertale Arena Box (x=220, y=185, w=200, h=155)
            val bx = 220f
            val by = 185f
            val bw = 200f
            val bh = 155f
            rect(bx, by, bw, bh, Color.Black)
            strokeRect(bx, by, bw, bh, RetroWhite, 4f)

            if (battle.phase == BattlePhase.ATTACK_BAR) {
                // Timing Meter
                val mx = bx + 16f
                val my = by + 45f
                val mw = bw - 32f
                val mh = 65f

                // Outer red, middle orange, center green zone
                rect(mx, my, mw, mh, Color(0xFFB71C1C))
                rect(mx + mw * 0.25f, my, mw * 0.5f, mh, Color(0xFFF57F17))
                rect(mx + mw * 0.43f, my, mw * 0.14f, mh, RetroGreen)

                // Moving line cursor
                val curX = mx + battle.barCursor * mw
                val curColor = if ((tick / 3) % 2 == 0L) Color.White else RetroYellow
                rect(curX - 3f, my - 8f, 6f, mh + 16f, curColor)
            } else if (battle.phase == BattlePhase.DODGE) {
                // Soul Heart
                if (battle.soulInvuln % 4 < 2) {
                    val sx = battle.soulX
                    val sy = battle.soulY
                    rect(sx + 2f, sy, 4f, 4f, RetroRed)
                    rect(sx + 8f, sy, 4f, 4f, RetroRed)
                    rect(sx, sy + 2f, 14f, 6f, RetroRed)
                    rect(sx + 2f, sy + 8f, 10f, 3f, RetroRed)
                    rect(sx + 4f, sy + 11f, 6f, 2f, RetroRed)
                    rect(sx + 6f, sy + 13f, 2f, 2f, RetroRed)
                }

                // Draw Bullets
                battle.bullets.forEach { bul ->
                    if (bul.type == "syringe") {
                        rect(bul.x, bul.y, bul.w, bul.h, Color.White)
                        rect(bul.x + 4f, bul.y + 2f, bul.w - 8f, bul.h - 4f, Color(0xFF76FF03))
                    } else if (bul.type == "pill") {
                        rect(bul.x, bul.y, bul.w / 2f, bul.h, Color(0xFFFF5252))
                        rect(bul.x + bul.w / 2f, bul.y, bul.w / 2f, bul.h, Color.White)
                    } else {
                        // Golden key
                        rect(bul.x, bul.y, bul.w, bul.h, Color(0xFFFFD740))
                    }
                }
            } else {
                // Heart in center of box
                rect(bx + bw / 2f - 4f, by + bh / 2f - 4f, 8f, 8f, RetroRed)
            }
        }
    }
}
