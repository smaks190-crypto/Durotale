package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DoorAction
import com.example.ui.theme.RetroBoxBlack
import com.example.ui.theme.RetroGreen
import com.example.ui.theme.RetroRed
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow

@Composable
fun RetroHUD(
    playerHP: Int,
    maxHP: Int,
    endingsCount: Int,
    isMuted: Boolean,
    doorCombo: List<DoorAction>,
    onToggleMute: () -> Unit,
    onResetCell: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player info and HP
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ПАЦИЕНТ #404",
                    color = RetroYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "УР 1",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ОЗ",
                        color = RetroWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    // HP Bar
                    Box(
                        modifier = Modifier
                            .width(76.dp)
                            .height(14.dp)
                            .background(RetroRed)
                            .border(1.dp, RetroWhite)
                    ) {
                        val fraction = (playerHP.toFloat() / maxHP.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .background(RetroYellow)
                        )
                    }
                    Text(
                        text = "$playerHP/$maxHP",
                        color = RetroWhite,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Top action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Endings badge
                val badgeColor = if (endingsCount >= 2) RetroYellow else RetroGreen
                Box(
                    modifier = Modifier
                        .background(Color(0xFF14141E), RoundedCornerShape(3.dp))
                        .border(1.dp, badgeColor, RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "КОНЦОВКИ: $endingsCount/2",
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Sound Mute Toggle
                Box(
                    modifier = Modifier
                        .testTag("btn_sound")
                        .clip(RoundedCornerShape(3.dp))
                        .background(RetroBoxBlack)
                        .border(1.dp, RetroWhite, RoundedCornerShape(3.dp))
                        .clickable { onToggleMute() }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isMuted) "🔇" else "🔊",
                        fontSize = 11.sp
                    )
                }

                // Restart in Cell Button
                Box(
                    modifier = Modifier
                        .testTag("btn_reset_cell")
                        .clip(RoundedCornerShape(3.dp))
                        .background(RetroBoxBlack)
                        .border(1.dp, RetroWhite, RoundedCornerShape(3.dp))
                        .clickable { onResetCell() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "↺ В КАМЕРУ",
                        color = RetroWhite,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Noise combo preview if player is making noise at door
        if (doorCombo.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(Color(0xEE0A0A12), RoundedCornerShape(2.dp))
                    .border(1.dp, Color(0xFF454555), RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ШУМ У ДВЕРИ:",
                    color = Color.Gray,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                doorCombo.forEachIndexed { i, act ->
                    val (label, bg, fg) = when (act) {
                        DoorAction.BANG -> Triple("🥊ТАРАБАНИТЬ", Color(0xFF4A0E0E), Color(0xFFFF8A80))
                        DoorAction.SCREAM -> Triple("🤬ОРАТЬ", Color(0xFF4A2F0E), Color(0xFFFFD54F))
                        DoorAction.KNOCK -> Triple("✊СТУК", Color(0xFF1B3B22), Color(0xFFA5D6A7))
                    }
                    Box(
                        modifier = Modifier
                            .background(bg, RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = label,
                            color = fg,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (i < doorCombo.size - 1) {
                        Text(
                            text = "→",
                            color = Color.DarkGray,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}
