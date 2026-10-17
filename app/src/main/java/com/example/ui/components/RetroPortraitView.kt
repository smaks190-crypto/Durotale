package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RetroBoxBlack
import com.example.ui.theme.RetroWhite

@Composable
fun RetroPortraitView(
    portraitType: String?,
    modifier: Modifier = Modifier
) {
    if (portraitType == null) return

    Box(
        modifier = modifier
            .size(68.dp)
            .background(RetroBoxBlack)
            .border(2.dp, RetroWhite)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scaleX = size.width / 64f
            val scaleY = size.height / 64f

            fun rect(x: Float, y: Float, w: Float, h: Float, color: Color) {
                drawRect(
                    color = color,
                    topLeft = Offset(x * scaleX, y * scaleY),
                    size = Size(w * scaleX, h * scaleY)
                )
            }

            if (portraitType.startsWith("player")) {
                // Hair or Doctor Cap
                if (portraitType == "player_doctor") {
                    rect(14f, 5f, 36f, 13f, Color.White)
                    rect(30f, 7f, 4f, 9f, Color(0xFFE53935))
                    rect(27f, 10f, 10f, 3f, Color(0xFFE53935))
                } else {
                    rect(14f, 8f, 36f, 12f, Color(0xFF6B4423))
                }

                // Face
                val skinColor = if (portraitType == "player_vegetable") Color(0xFFC8E6C9) else Color(0xFFFFCCAA)
                rect(16f, 18f, 32f, 26f, skinColor)

                // Eyes
                if (portraitType == "player_vegetable") {
                    // Derpy crossed eyes
                    rect(20f, 25f, 6f, 6f, Color.Black)
                    rect(38f, 27f, 6f, 6f, Color.Black)
                    rect(21f, 26f, 2f, 2f, Color.White)
                    rect(41f, 29f, 2f, 2f, Color.White)
                    // Drool
                    rect(34f, 39f, 4f, 11f, Color(0xFF4FC3F7))
                } else if (portraitType == "player_shock" || portraitType == "player_angry") {
                    rect(20f, 24f, 8f, 8f, Color.White)
                    rect(36f, 24f, 8f, 8f, Color.White)
                    rect(23f, 26f, 3f, 3f, Color.Black)
                    rect(39f, 26f, 3f, 3f, Color.Black)
                } else {
                    rect(22f, 26f, 4f, 6f, Color.Black)
                    rect(38f, 26f, 4f, 6f, Color.Black)
                }

                // Mouth
                if (portraitType == "player_shock") {
                    rect(26f, 36f, 12f, 6f, Color.Black)
                } else if (portraitType == "player_smirk" || portraitType == "player_doctor") {
                    rect(24f, 37f, 14f, 3f, Color.Black)
                    rect(36f, 35f, 3f, 3f, Color.Black)
                } else {
                    rect(26f, 37f, 12f, 3f, Color.Black)
                }

                // Shoulders / Clothes
                if (portraitType == "player_doctor") {
                    rect(10f, 44f, 44f, 18f, Color(0xFFE0F2F1))
                    rect(29f, 44f, 6f, 18f, Color(0xFF26A69A))
                } else {
                    rect(10f, 44f, 44f, 18f, Color(0xFFEEEEEE))
                    rect(12f, 50f, 40f, 3f, Color(0xFF999999))
                }
            } else if (portraitType.startsWith("orderly")) {
                // Medical Cap with Red Cross
                rect(16f, 6f, 32f, 12f, Color.White)
                rect(30f, 8f, 4f, 8f, Color(0xFFE53935))
                rect(28f, 10f, 8f, 4f, Color(0xFFE53935))

                // Burly Face
                rect(14f, 18f, 36f, 28f, Color(0xFFF5CBA7))

                // Mustache
                rect(22f, 34f, 20f, 5f, Color(0xFF5D4037))

                // Eyes
                if (portraitType == "orderly_angry") {
                    rect(20f, 24f, 7f, 3f, Color.Black)
                    rect(37f, 24f, 7f, 3f, Color.Black)
                    rect(22f, 27f, 4f, 4f, Color(0xFFE53935))
                    rect(38f, 27f, 4f, 4f, Color(0xFFE53935))
                } else if (portraitType == "orderly_panic") {
                    rect(19f, 23f, 9f, 9f, Color.White)
                    rect(36f, 23f, 9f, 9f, Color.White)
                    rect(22f, 26f, 3f, 3f, Color.Black)
                    rect(39f, 26f, 3f, 3f, Color.Black)
                } else {
                    rect(21f, 26f, 5f, 4f, Color.Black)
                    rect(38f, 26f, 5f, 4f, Color.Black)
                }

                // Coat
                rect(10f, 46f, 44f, 18f, Color(0xFFB2DFDB))
            } else if (portraitType == "syringe") {
                // Giant Glowing Syringe
                rect(28f, 8f, 8f, 12f, Color(0xFFB0BEC5))
                rect(22f, 20f, 20f, 28f, Color.White)
                rect(25f, 24f, 14f, 20f, Color(0xFF76FF03))
                rect(31f, 48f, 2f, 12f, Color.White)
            }
        }
    }
}
