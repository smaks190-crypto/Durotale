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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.BattlePhase
import com.example.model.BattleState
import com.example.ui.render.PixelSprites
import com.example.ui.render.drawPixelSprite
import com.example.ui.theme.RetroBoxBlack
import com.example.ui.theme.RetroGreen
import com.example.ui.theme.RetroOrange
import com.example.ui.theme.RetroRed
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow
import kotlin.math.cos
import kotlin.math.sin

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

            // Undertale black background with green starfield/grid lines
            drawRect(color = Color.Black)
            for (gx in 40 until 600 step 40) {
                strokeRect(gx.toFloat(), 15f, 40f, 150f, Color(0xFF0A1F10), 1f)
            }

            // ENEMY: САНИТАР МИХАЛЫЧ (Undertale Monster Boss style)
            val shakeOffset = if (battle.enemyShake > 0) ((Math.random() - 0.5) * 16.0).toFloat() else 0f
            val bossBaseX = 265f + shakeOffset
            val bossBaseY = 16f

            // Breathing / Sinuous Idle Animation
            val breath = sin(tick * 0.08f).toFloat() * 2.5f
            val armSwing = cos(tick * 0.08f).toFloat() * 2f

            // Enemy HP Bar above head
            rect(250f, 10f, 140f, 9f, Color(0xFF333333))
            val hpRatio = (battle.enemyHP.toFloat() / battle.enemyMaxHP.toFloat()).coerceIn(0f, 1f)
            rect(250f, 10f, 140f * hpRatio, 9f, RetroGreen)
            strokeRect(250f, 10f, 140f, 9f, RetroWhite, 1.5f)

            val bossPixelScale = 2.1f * scaleX

            // 1. Draw Torso
            val torsoTopLeft = Offset((bossBaseX + 16f) * scaleX, (bossBaseY + 44f + breath * 0.5f) * scaleY)
            drawPixelSprite(
                matrix = PixelSprites.BOSS_TORSO,
                palette = PixelSprites.BossMonochromePalette,
                topLeft = torsoTopLeft,
                pixelSize = bossPixelScale
            )

            // 2. Draw Right Arm with Giant Syringe
            val rightArmTopLeft = Offset((bossBaseX + 70f + armSwing) * scaleX, (bossBaseY + 38f + breath) * scaleY)
            drawPixelSprite(
                matrix = PixelSprites.BOSS_ARM_SYRINGE,
                palette = PixelSprites.BossMonochromePalette,
                topLeft = rightArmTopLeft,
                pixelSize = bossPixelScale
            )

            // 3. Draw Left Arm with Key Ring
            val leftArmTopLeft = Offset((bossBaseX - 12f - armSwing) * scaleX, (bossBaseY + 42f + breath) * scaleY)
            drawPixelSprite(
                matrix = PixelSprites.BOSS_ARM_KEYS,
                palette = PixelSprites.BossMonochromePalette,
                topLeft = leftArmTopLeft,
                pixelSize = bossPixelScale
            )

            // 4. Draw Head with emotional expression matrix
            val headMatrix = when (battle.bossEmotion) {
                "shock" -> PixelSprites.BOSS_HEAD_SHOCKED
                "hurt" -> PixelSprites.BOSS_HEAD_HURT
                else -> PixelSprites.BOSS_HEAD_NORMAL
            }
            val headTopLeft = Offset((bossBaseX + 18f) * scaleX, (bossBaseY + breath) * scaleY)
            drawPixelSprite(
                matrix = headMatrix,
                palette = PixelSprites.BossMonochromePalette,
                topLeft = headTopLeft,
                pixelSize = bossPixelScale
            )

            // CURVED SLASH EFFECT ON HIT
            if (battle.phase == BattlePhase.SLASH_ANIM) {
                val p = battle.slashProgress.coerceIn(0f, 1f)
                val slashStart = Offset((bossBaseX + 10f) * scaleX, (bossBaseY + 20f) * scaleY)
                val slashControl = Offset((bossBaseX + 50f + p * 20f) * scaleX, (bossBaseY + 80f) * scaleY)
                val slashEnd = Offset(
                    (bossBaseX + 10f + p * 80f) * scaleX,
                    (bossBaseY + 20f + p * 120f) * scaleY
                )

                val slashPath = Path().apply {
                    moveTo(slashStart.x, slashStart.y)
                    quadraticTo(slashControl.x, slashControl.y, slashEnd.x, slashEnd.y)
                }

                // Outer red slash glow
                drawPath(
                    path = slashPath,
                    color = Color(0xFFFF1744),
                    style = Stroke(width = 7f * scaleX)
                )
                // Inner white slice
                drawPath(
                    path = slashPath,
                    color = Color.White,
                    style = Stroke(width = 3f * scaleX)
                )

                // BOUNCING PIXEL DAMAGE NUMBER JUMP
                val dmgText = battle.damageText
                if (dmgText != null) {
                    val dmgX = (bossBaseX + 32f) * scaleX
                    val dmgY = (bossBaseY + 36f + battle.damageTextY) * scaleY

                    // Draw pixelated damage indicator
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(dmgX - 4f * scaleX, dmgY - 2f * scaleY),
                        size = Size(46f * scaleX, 22f * scaleY)
                    )
                    drawRect(
                        color = RetroYellow,
                        topLeft = Offset(dmgX - 4f * scaleX, dmgY - 2f * scaleY),
                        size = Size(46f * scaleX, 22f * scaleY),
                        style = Stroke(width = 2f * scaleX)
                    )
                    // Digits block representation
                    drawRect(
                        color = RetroRed,
                        topLeft = Offset(dmgX + 4f * scaleX, dmgY + 6f * scaleY),
                        size = Size(34f * scaleX, 10f * scaleY)
                    )
                }
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
