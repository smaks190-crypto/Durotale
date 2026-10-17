package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameChoice
import com.example.ui.theme.RetroBoxBlack
import com.example.ui.theme.RetroRed
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow

@Composable
fun RetroDialogueBox(
    speaker: String,
    portrait: String?,
    dialogueText: String,
    choices: List<GameChoice>,
    selectedChoiceIndex: Int,
    canAdvanceDialogue: Boolean,
    onChoiceSelected: (Int) -> Unit,
    onAdvanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "prompt_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 145.dp)
            .background(RetroBoxBlack, RoundedCornerShape(2.dp))
            .border(3.dp, RetroWhite, RoundedCornerShape(2.dp))
            .clickable {
                if (choices.isEmpty()) onAdvanceClick()
            }
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Portrait if present
                if (portrait != null) {
                    RetroPortraitView(portraitType = portrait)
                }

                // Dialogue text content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (speaker.isNotEmpty()) {
                        Text(
                            text = speaker,
                            color = RetroYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = dialogueText,
                        color = RetroWhite,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("dialogue_text")
                    )

                    // Undertale Choices Grid
                    if (choices.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            choices.forEachIndexed { index, choice ->
                                val isSelected = index == selectedChoiceIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onChoiceSelected(index) }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isSelected) "♥" else " ",
                                        color = if (isSelected) RetroRed else Color.Transparent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = choice.label,
                                        color = if (isSelected) RetroYellow else RetroWhite,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Prompt indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (choices.isNotEmpty()) "Выбор: [СТРЕЛКИ / КАСАНИЕ]" else "",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (canAdvanceDialogue && choices.isEmpty()) {
                    Text(
                        text = "[Z / ДАЛЕЕ] ▼",
                        color = RetroYellow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .alpha(alphaAnim)
                            .testTag("continue_prompt")
                    )
                }
            }
        }
    }
}
