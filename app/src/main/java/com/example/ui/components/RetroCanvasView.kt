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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DustParticle
import com.example.model.GameMode
import com.example.model.OrderlyState
import com.example.model.PlayerState
import com.example.ui.render.PixelSprites
import com.example.ui.render.drawPixelSprite
import com.example.ui.theme.RetroBoxBlack
import com.example.ui.theme.RetroFloor
import com.example.ui.theme.RetroPaddedWall
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow
import kotlin.random.Random

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
    dustParticles: List<DustParticle>,
    screenShakeTick: Int,
    screenShakeIntensity: Float,
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
            val pixelScale = 1.9f * scaleX

            // Screen shake transform
            val shakeX = if (screenShakeTick > 0) {
                (Random.nextDouble(-1.0, 1.0) * screenShakeIntensity * scaleX).toFloat()
            } else 0f
            val shakeY = if (screenShakeTick > 0) {
                (Random.nextDouble(-1.0, 1.0) * screenShakeIntensity * scaleY).toFloat()
            } else 0f

            withTransform({
                translate(shakeX, shakeY)
            }) {
                // Outer black void
                drawRect(color = Color.Black)

                // 1. Corridor below door
                drawRect(
                    color = Color(0xFF14141E),
                    topLeft = Offset(245f * scaleX, 310f * scaleY),
                    size = Size(150f * scaleX, 90f * scaleY)
                )
                // Corridor tiles
                for (cy in 315..395 step 16) {
                    drawRect(
                        color = Color(0xFF222234),
                        topLeft = Offset(245f * scaleX, cy * scaleY),
                        size = Size(150f * scaleX, 16f * scaleY),
                        style = Stroke(width = 1.2f * scaleX)
                    )
                }

                // 2. Main Padded Room Background
                val roomLeft = 120f * scaleX
                val roomTop = 30f * scaleY
                val roomWidth = 400f * scaleX
                val roomHeight = 285f * scaleY

                // Back Wall base
                drawRect(
                    color = RetroPaddedWall,
                    topLeft = Offset(roomLeft, roomTop),
                    size = Size(roomWidth, 68f * scaleY)
                )

                // Soft Floor base
                drawRect(
                    color = RetroFloor,
                    topLeft = Offset(roomLeft, 98f * scaleY),
                    size = Size(roomWidth, 217f * scaleY)
                )

                // Diamond/Quilted Wall Stitching & 3D Shading
                val stitchSpacing = 36f * scaleX
                for (wx in 120 until 520 step 36) {
                    for (wy in 30 until 98 step 24) {
                        val cx = wx * scaleX
                        val cy = wy * scaleY
                        val cw = 36f * scaleX
                        val ch = 24f * scaleY

                        // Top & Left highlight line
                        drawLine(
                            color = Color(0xFFEEF2F7),
                            start = Offset(cx, cy),
                            end = Offset(cx + cw, cy),
                            strokeWidth = 1.2f * scaleX
                        )
                        drawLine(
                            color = Color(0xFFEEF2F7),
                            start = Offset(cx, cy),
                            end = Offset(cx, cy + ch),
                            strokeWidth = 1.2f * scaleX
                        )

                        // Bottom & Right drop shadow seam
                        drawLine(
                            color = Color(0xFF8A97A8),
                            start = Offset(cx, cy + ch),
                            end = Offset(cx + cw, cy + ch),
                            strokeWidth = 1.5f * scaleX
                        )
                        drawLine(
                            color = Color(0xFF8A97A8),
                            start = Offset(cx + cw, cy),
                            end = Offset(cx + cw, cy + ch),
                            strokeWidth = 1.5f * scaleX
                        )

                        // Button Rivet in center of cushion
                        drawCircle(
                            color = Color(0xFF6F7E91),
                            radius = 2.4f * scaleX,
                            center = Offset(cx + cw / 2f, cy + ch / 2f)
                        )
                        drawCircle(
                            color = Color(0xFFBAC5D4),
                            radius = 1.2f * scaleX,
                            center = Offset(cx + cw / 2f - 0.5f, cy + ch / 2f - 0.5f)
                        )
                    }
                }

                // Quilted Floor Grid & Tufting
                for (fx in 120 until 520 step 48) {
                    for (fy in 98 until 315 step 40) {
                        val fcx = fx * scaleX
                        val fcy = fy * scaleY
                        val fcw = 48f * scaleX
                        val fch = 40f * scaleY

                        // Seam lines
                        drawRect(
                            color = Color(0xFF9EAAB9),
                            topLeft = Offset(fcx, fcy),
                            size = Size(fcw, fch),
                            style = Stroke(width = 1.2f * scaleX)
                        )

                        // Floor button tufts
                        drawCircle(
                            color = Color(0xFF7E8B9C),
                            radius = 2.5f * scaleX,
                            center = Offset(fcx + fcw / 2f, fcy + fch / 2f)
                        )
                    }
                }

                // REQUIREMENT 2: SCRATCHED CIPHER WALL HINT (I, II, III, IV, V with Fist / Scream icons)
                val hintStartX = 415f * scaleX
                val hintStartY = 42f * scaleY
                val numScale = 1.1f * scaleX

                val numerals = listOf(
                    PixelSprites.ROMAN_I,
                    PixelSprites.ROMAN_II,
                    PixelSprites.ROMAN_III,
                    PixelSprites.ROMAN_IV,
                    PixelSprites.ROMAN_V
                )
                val icons = listOf(
                    PixelSprites.ICON_FIST,
                    PixelSprites.ICON_SCREAM,
                    PixelSprites.ICON_FIST,
                    PixelSprites.ICON_FIST,
                    PixelSprites.ICON_SCREAM
                )

                // Background scratch patch
                drawRect(
                    color = Color(0x33455A64),
                    topLeft = Offset(hintStartX - 4f * scaleX, hintStartY - 3f * scaleY),
                    size = Size(96f * scaleX, 26f * scaleY)
                )

                for (idx in 0 until 5) {
                    val nx = hintStartX + idx * 19f * scaleX
                    drawPixelSprite(
                        matrix = numerals[idx],
                        palette = PixelSprites.WallHintPalette,
                        topLeft = Offset(nx, hintStartY),
                        pixelSize = numScale
                    )
                    drawPixelSprite(
                        matrix = icons[idx],
                        palette = mapOf('#' to Color(0xFFD32F2F), '.' to Color.Transparent),
                        topLeft = Offset(nx - 1f * scaleX, hintStartY + 10f * scaleY),
                        pixelSize = numScale
                    )
                }

                // Corner Bed Mattress & Pillow (top-left)
                drawRect(
                    color = Color(0xFFD6DCE5),
                    topLeft = Offset(132f * scaleX, 98f * scaleY),
                    size = Size(58f * scaleX, 36f * scaleY)
                )
                drawRect(
                    color = Color(0xFF9AA5B4),
                    topLeft = Offset(132f * scaleX, 98f * scaleY),
                    size = Size(58f * scaleX, 36f * scaleY),
                    style = Stroke(width = 1.5f * scaleX)
                )
                // White soft pillow
                drawRect(
                    color = Color.White,
                    topLeft = Offset(136f * scaleX, 102f * scaleY),
                    size = Size(36f * scaleX, 22f * scaleY)
                )
                drawRect(
                    color = Color(0xFFB0BEC5),
                    topLeft = Offset(136f * scaleX, 102f * scaleY),
                    size = Size(36f * scaleX, 22f * scaleY),
                    style = Stroke(width = 1.2f * scaleX)
                )

                // Overhead Ceiling Lamp & Light Cone
                val lampX = 320f * scaleX
                val lampY = 30f * scaleY
                drawRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(lampX - 14f * scaleX, lampY - 4f * scaleY),
                    size = Size(28f * scaleX, 8f * scaleY)
                )
                drawCircle(
                    color = Color(0xFFFFEB3B),
                    radius = 4f * scaleX,
                    center = Offset(lampX, lampY + 4f * scaleY)
                )

                // Translucent Light Cone onto the floor
                val lightPath = Path().apply {
                    moveTo(lampX - 10f * scaleX, lampY + 6f * scaleY)
                    lineTo(lampX + 10f * scaleX, lampY + 6f * scaleY)
                    lineTo(lampX + 150f * scaleX, 290f * scaleY)
                    lineTo(lampX - 150f * scaleX, 290f * scaleY)
                    close()
                }
                drawPath(
                    path = lightPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0x35FFEE58), Color(0x10FFF9C4), Color(0x00FFF59D)),
                        start = Offset(lampX, lampY),
                        end = Offset(lampX, 290f * scaleY)
                    )
                )

                // Radial Vignette Shading on Corners
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x66000000)),
                        center = Offset(320f * scaleX, 180f * scaleY),
                        radius = 280f * scaleX
                    ),
                    topLeft = Offset(roomLeft, roomTop),
                    size = Size(roomWidth, roomHeight)
                )

                // Heavy White Asylum Border Frame
                drawRect(
                    color = RetroWhite,
                    topLeft = Offset(roomLeft, roomTop),
                    size = Size(roomWidth, roomHeight),
                    style = Stroke(width = 5f * scaleX)
                )

                // DOOR RENDERING (REQUIREMENT 3: Open during drag cutscene, closed with slam effect)
                val doorX = 280f * scaleX
                val doorY = 305f * scaleY
                val doorWidth = 80f * scaleX
                val doorHeight = 20f * scaleY

                if (!doorOpen) {
                    // Closed Heavy Steel Door
                    drawRect(
                        color = Color(0xFF455A64),
                        topLeft = Offset(doorX, doorY),
                        size = Size(doorWidth, doorHeight)
                    )
                    drawRect(
                        color = Color(0xFF263238),
                        topLeft = Offset(doorX, doorY),
                        size = Size(doorWidth, doorHeight),
                        style = Stroke(width = 2f * scaleX)
                    )

                    // Door Steel Rivets
                    for (rx in 284..356 step 12) {
                        drawCircle(
                            color = Color(0xFF90A4AE),
                            radius = 1.4f * scaleX,
                            center = Offset(rx * scaleX, (doorY + 4f * scaleY))
                        )
                        drawCircle(
                            color = Color(0xFF90A4AE),
                            radius = 1.4f * scaleX,
                            center = Offset(rx * scaleX, (doorY + 16f * scaleY))
                        )
                    }

                    // Barred Window in door
                    val winX = 306f * scaleX
                    val winY = 308f * scaleY
                    val winW = 28f * scaleX
                    val winH = 13f * scaleY

                    val winBg = if (orderlyAtWindow) Color(0xFFF5CBA7) else Color(0xFF0F1416)
                    drawRect(color = winBg, topLeft = Offset(winX, winY), size = Size(winW, winH))
                    drawRect(color = Color(0xFF90A4AE), topLeft = Offset(winX, winY), size = Size(winW, winH), style = Stroke(width = 1.5f * scaleX))

                    // Window Iron Bars
                    drawLine(color = Color(0xFF78909C), start = Offset(winX + 9f * scaleX, winY), end = Offset(winX + 9f * scaleX, winY + winH), strokeWidth = 1.5f * scaleX)
                    drawLine(color = Color(0xFF78909C), start = Offset(winX + 19f * scaleX, winY), end = Offset(winX + 19f * scaleX, winY + winH), strokeWidth = 1.5f * scaleX)

                    if (orderlyAtWindow) {
                        // Orderly's angry eyes peeking through bars
                        drawRect(color = Color.Black, topLeft = Offset(winX + 5f * scaleX, winY + 4f * scaleY), size = Size(4f * scaleX, 4f * scaleY))
                        drawRect(color = Color.Black, topLeft = Offset(winX + 20f * scaleX, winY + 4f * scaleY), size = Size(4f * scaleX, 4f * scaleY))
                        drawRect(color = Color(0xFF4E342E), topLeft = Offset(winX + 8f * scaleX, winY + 9f * scaleY), size = Size(12f * scaleX, 3f * scaleY))
                    }
                } else {
                    // Swung-Open Doorway!
                    drawRect(
                        color = Color(0xFF14141E),
                        topLeft = Offset(doorX + 2f * scaleX, doorY - 1f * scaleY),
                        size = Size(76f * scaleX, 22f * scaleY)
                    )
                    // Swung steel door leaf on hinge to the right
                    drawRect(
                        color = Color(0xFF455A64),
                        topLeft = Offset(358f * scaleX, 312f * scaleY),
                        size = Size(14f * scaleX, 48f * scaleY)
                    )
                    drawRect(
                        color = Color(0xFF263238),
                        topLeft = Offset(358f * scaleX, 312f * scaleY),
                        size = Size(14f * scaleX, 48f * scaleY),
                        style = Stroke(width = 1.5f * scaleX)
                    )
                }

                // DRAW DUST / SPARK PARTICLES
                dustParticles.forEach { p ->
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                        radius = p.size * scaleX,
                        center = Offset(p.x * scaleX, p.y * scaleY)
                    )
                }

                // DRAW CHARACTERS WITH DETAILED PIXEL SPRITES

                // 1. Orderly 1 (Mihalych)
                val oFrame = orderly.animFrame
                val oMatrix = when {
                    !orderly.hasCoat -> PixelSprites.ORDERLY_STRIPPED_DOWN
                    oFrame == 1 -> PixelSprites.ORDERLY_DOWN_STEP1
                    oFrame == 3 -> PixelSprites.ORDERLY_DOWN_STEP2
                    else -> PixelSprites.ORDERLY_DOWN_IDLE
                }

                if (mode == GameMode.CUTSCENE_DRAG || orderlyInside) {
                    val oFlip = orderly.dir == "right"
                    val oTopLeft = Offset(orderly.x * scaleX, orderly.y * scaleY)
                    drawPixelSprite(
                        matrix = oMatrix,
                        palette = PixelSprites.OrderlyPalette,
                        topLeft = oTopLeft,
                        pixelSize = pixelScale,
                        flipX = oFlip
                    )

                    // Emote bubble above head (?, !)
                    if (orderly.bubbleSymbol == "?") {
                        drawPixelSprite(
                            matrix = PixelSprites.EMOTE_QUESTION,
                            palette = PixelSprites.EmotePalette,
                            topLeft = Offset(orderly.x * scaleX + 8f * scaleX, orderly.y * scaleY - 16f * scaleY),
                            pixelSize = 1.4f * scaleX
                        )
                    } else if (orderly.bubbleSymbol == "!") {
                        drawPixelSprite(
                            matrix = PixelSprites.EMOTE_EXCLAMATION,
                            palette = mapOf('#' to Color(0xFFFF1744), '.' to Color.Transparent),
                            topLeft = Offset(orderly.x * scaleX + 8f * scaleX, orderly.y * scaleY - 16f * scaleY),
                            pixelSize = 1.4f * scaleX
                        )
                    }
                }

                // 2. Orderly 2 (Petrovich / Newbie)
                if (orderly2Visible) {
                    val o2Matrix = PixelSprites.ORDERLY_DOWN_IDLE
                    drawPixelSprite(
                        matrix = o2Matrix,
                        palette = PixelSprites.OrderlyPalette,
                        topLeft = Offset(orderly2.x * scaleX, orderly2.y * scaleY),
                        pixelSize = pixelScale
                    )
                }

                // 3. Player Character (Patient in straitjacket or Doctor in coat)
                val pFrame = player.animFrame
                val pMatrix = when {
                    player.hasCoat -> PixelSprites.PLAYER_DOCTOR_DOWN
                    player.dir == "up" -> PixelSprites.PLAYER_UP_IDLE
                    player.dir == "left" || player.dir == "right" -> {
                        if (pFrame == 1 || pFrame == 3) PixelSprites.PLAYER_SIDE_STEP1 else PixelSprites.PLAYER_SIDE_IDLE
                    }
                    else -> {
                        if (pFrame == 1) PixelSprites.PLAYER_DOWN_STEP1
                        else if (pFrame == 3) PixelSprites.PLAYER_DOWN_STEP2
                        else PixelSprites.PLAYER_DOWN_IDLE
                    }
                }

                val pFlip = player.dir == "right"
                val pBob = if (player.moving && (pFrame == 1 || pFrame == 3)) 1.5f * scaleY else 0f
                val pTopLeft = Offset(player.x * scaleX, player.y * scaleY + pBob)

                drawPixelSprite(
                    matrix = pMatrix,
                    palette = PixelSprites.PlayerPalette,
                    topLeft = pTopLeft,
                    pixelSize = pixelScale,
                    flipX = pFlip
                )
            }
        }

        // Action prompt badge
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
