package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.RetroBoxBlack
import com.example.ui.theme.RetroGreen
import com.example.ui.theme.RetroOrange
import com.example.ui.theme.RetroRed
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow

@Composable
fun HintsDialog(
    onDismiss: () -> Unit,
    onRestartIntro: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(RetroBoxBlack)
                .border(3.dp, RetroWhite, RoundedCornerShape(4.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "★ ПАМЯТКА ПАЦИЕНТА",
                        color = RetroYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "[X]",
                        color = RetroRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                // Ending 1 card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x2200E676), RoundedCornerShape(3.dp))
                        .border(1.dp, RetroGreen, RoundedCornerShape(3.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "1. КОНЦОВКА «КТО В ХАЛАТЕ — ТОТ И ВРАЧ»:",
                        color = RetroGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Подойдите к двери камеры → выберите «Постучать» → «Вы выронили телефон у меня в камере!». Санитар зайдёт внутрь — начнётся битва Undertale! Победите его сердечком, заберите халат и заприте Михалыча в камере!",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Ending 2 card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x22BA68C8), RoundedCornerShape(3.dp))
                        .border(1.dp, Color(0xFFBA68C8), RoundedCornerShape(3.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "2. СЕКРЕТНАЯ КОНЦОВКА «ОВОЩ»:",
                        color = Color(0xFFCE93D8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Подойдите к двери и наберите комбинацию шума подряд:\n1) Тарабанить → 2) Орать что не псих → 3) Тарабанить → 4) Тарабанить → 5) Орать!",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("btn_modal_restart")
                            .clip(RoundedCornerShape(3.dp))
                            .background(RetroBoxBlack)
                            .border(1.dp, RetroOrange, RoundedCornerShape(3.dp))
                            .clickable {
                                onDismiss()
                                onRestartIntro()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "КАТСЦЕНА ЗАНОВО",
                            color = RetroOrange,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("btn_modal_ok")
                            .clip(RoundedCornerShape(3.dp))
                            .background(RetroWhite)
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ПОНЯТНО",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
