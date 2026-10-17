package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.model.OrderlyState
import com.example.model.PlayerState
import com.example.ui.theme.RetroBoxBlack
import com.example.ui.theme.RetroFloor
import com.example.ui.theme.RetroPaddedWall
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow

@Composable
fun RetroCanvasView(
    mode: GameMode,
    player: PlayerState,
    orderly: OrderlyState,
    orderly2: OrderlyState,
    orderly2Visible: Boolean,
    doorOpen: Boolean,
    orderlyAtWindow: Boolean,
    orderlyInside: Boolean,
    isVegetable: Boolean,
    interactPrompt: String?,
    tick: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .background(RetroBoxBlack, RoundedCornerShape(2.dp))
            .border(3.dp, RetroWhite, RoundedCornerShape(2.dp))
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

            // Outer void
            drawRect(color = Color.Black)

            // Corridor below door
            rect(250f, 315f, 140f, 85f, Color(0xFF181824))
            for (y in 320..390 step 20) {
                strokeRect(250f, y.toFloat(), 140f, 20f, Color(0xFF252538), 1.5f)
            }

            // Back Padded Wall (Top)
            rect(120f, 30f, 400f, 65f, RetroPaddedWall)

            // Side Padded Walls Border
            strokeRect(120f, 30f, 400f, 290f, RetroWhite, 6f)

            // Padded Cushion Pattern on Top Wall
            for (x in 120 until 520 step 40) {
                for (y in 30 until 90 step 30) {
                    strokeRect(x.toFloat(), y.toFloat(), 40f, 30f, Color(0xFF9AA5B4), 1.5f)
                    // Button tufts
                    rect(x + 18f, y + 13f, 4f, 4f, Color(0xFF7B8798))
                }
            }

            // Soft Quilted Floor
            rect(124f, 95f, 392f, 220f, RetroFloor)

            // Floor quilted grid
            for (x in 124 until 516 step 48) {
                for (y in 95 until 315 step 44) {
                    strokeRect(x.toFloat(), y.toFloat(), 48f, 44f, Color(0xFFA3ADBC), 1.5f)
                    rect(x + 22f, y + 20f, 4f, 4f, Color(0xFF8E99A8))
                }
            }

            // Scratched secret hint on top-right wall pad
            rect(425f, 52f, 32f, 20f, Color(0xFF5C677D))
            rect(429f, 57f, 24f, 2f, RetroPaddedWall)
            rect(429f, 63f, 18f, 2f, RetroPaddedWall)

            // Soft Pillow in top-left corner
            rect(140f, 102f, 42f, 28f, Color.White)
            strokeRect(140f, 102f, 42f, 28f, Color(0xFF7B8798), 1.5f)

            // Door rendering
            if (!doorOpen && mode != GameMode.CUTSCENE_DRAG) {
                // Closed heavy steel door
                rect(280f, 306f, 80f, 18f, Color(0xFF455A64))
                strokeRect(280f, 306f, 80f, 18f, RetroWhite, 2f)

                // Small barred window in door
                val winColor = if (orderlyAtWindow) Color(0xFFF5CBA7) else Color(0xFF11171A)
                rect(306f, 309f, 28f, 11f, winColor)
                rect(314f, 309f, 2f, 11f, Color(0xFF90A4AE))
                rect(324f, 309f, 2f, 11f, Color(0xFF90A4AE))

                if (orderlyAtWindow) {
                    rect(311f, 313f, 3f, 3f, Color.Black)
                    rect(326f, 313f, 3f, 3f, Color.Black)
                }
            } else {
                // Open doorway
                rect(282f, 305f, 76f, 20f, Color(0xFF181824))
                // Swung open door leaf
                rect(358f, 312f, 14f, 48f, Color(0xFF455A64))
                strokeRect(358f, 312f, 14f, 48f, RetroWhite, 2f)
            }

            // DRAW ORDERLIES
            fun drawOrderly(ox: Float, oy: Float, hasCoat: Boolean, isInjecting: Boolean, isLocked: Boolean) {
                // Shadow
                rect(ox + 4f, oy + 35f, 24f, 6f, Color(0x66000000))
                // Legs
                rect(ox + 7f, oy + 28f, 7f, 10f, Color(0xFF37474F))
                rect(ox + 18f, oy + 28f, 7f, 10f, Color(0xFF37474F))

                // Coat or Telnyashka
                if (hasCoat) {
                    rect(ox + 3f, oy + 14f, 26f, 16f, Color(0xFFE0F2F1))
                    rect(ox + 14f, oy + 14f, 4f, 16f, Color(0xFF80CBC4))
                } else {
                    rect(ox + 4f, oy + 14f, 24f, 15f, Color.White)
                    for (sy in 16..26 step 4) {
                        rect(ox + 4f, sy.toFloat(), 24f, 2f, Color(0xFF1E88E5))
                    }
                }

                // Head
                rect(ox + 6f, oy + 4f, 20f, 11f, Color(0xFFF5CBA7))

                // Cap
                if (hasCoat) {
                    rect(ox + 6f, oy, 20f, 5f, Color.White)
                    rect(ox + 14f, oy + 1f, 4f, 3f, Color(0xFFE53935))
                }

                // Eyes & Mustache
                rect(ox + 9f, oy + 7f, 3f, 3f, Color.Black)
                rect(ox + 20f, oy + 7f, 3f, 3f, Color.Black)
                rect(ox + 11f, oy + 11f, 10f, 2f, Color(0xFF4E342E))

                if (isLocked) {
                    // Panic question mark
                    rect(ox + 10f, oy - 8f, 4f, 4f, RetroYellow)
                }
                if (isInjecting) {
                    // Glowing syringe
                    rect(ox + 28f, oy + 10f, 6f, 14f, Color(0xFF76FF03))
                    rect(ox + 30f, oy + 4f, 2f, 6f, Color.White)
                }
            }

            if (mode == GameMode.CUTSCENE_DRAG) {
                drawOrderly(orderly.x, orderly.y, true, false, false)
                if (orderly2Visible) {
                    drawOrderly(orderly2.x, orderly2.y, true, false, false)
                }
            } else if (orderlyInside) {
                val isInjecting = orderly.state == "injecting"
                val isLocked = orderly.state == "stripped_locked"
                drawOrderly(orderly.x, orderly.y, orderly.hasCoat, isInjecting, isLocked)
                if (orderly2Visible) {
                    drawOrderly(orderly2.x, orderly2.y, true, isInjecting, false)
                }
            }

            // DRAW PLAYER
            val bob = if (player.moving && player.animFrame == 1) 2f else 0f
            val px = player.x
            val py = player.y

            // Clothes
            if (player.hasCoat) {
                rect(px + 2f, py + 13f + bob, 24f, 15f, Color(0xFFE0F2F1))
                rect(px + 12f, py + 13f + bob, 4f, 15f, Color(0xFF26A69A))
            } else {
                rect(px + 3f, py + 13f + bob, 22f, 14f, Color.White)
                rect(px + 3f, py + 18f + bob, 22f, 3f, Color(0xFF78909C))
                rect(px + 9f, py + 13f + bob, 3f, 14f, Color(0xFF78909C))
            }

            // Head
            val skin = if (isVegetable) Color(0xFFA5D6A7) else Color(0xFFFFCCAA)
            rect(px + 4f, py + 2f + bob, 20f, 13f, skin)

            // Cap or Hair
            if (player.hasCoat) {
                rect(px + 4f, py - 2f + bob, 20f, 6f, Color.White)
                rect(px + 12f, py - 1f + bob, 4f, 4f, Color(0xFFE53935))
            } else {
                rect(px + 3f, py + bob, 22f, 5f, Color(0xFF5D4037))
            }

            // Eyes
            if (player.dir != "up") {
                rect(px + 8f, py + 7f + bob, 3f, 4f, Color.Black)
                rect(px + 17f, py + 7f + bob, 3f, 4f, Color.Black)
            }

            // Legs
            val legOffset = if (player.moving && player.animFrame == 1) 3f else 0f
            rect(px + 6f, py + 27f, 6f, 8f - legOffset, Color(0xFF37474F))
            rect(px + 16f, py + 27f, 6f, 8f - (3f - legOffset), Color(0xFF37474F))
        }

        // Action hint prompt badge
        if (interactPrompt != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .background(Color(0xF0000000), RoundedCornerShape(2.dp))
                    .border(2.dp, RetroYellow, RoundedCornerShape(2.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("interact_hint")
            ) {
                Text(
                    text = interactPrompt,
                    color = RetroYellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
