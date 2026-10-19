package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.model.ParsedOperationItem
import com.example.domain.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonRose
import com.example.ui.theme.SlateBorderDark
import com.example.ui.theme.SlateCardDark
import com.example.ui.theme.SlateSurfaceDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.voice.SpeechRecognitionManager
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceInputMorphingFab(
    isVoiceActive: Boolean,
    isListening: Boolean,
    rmsLevel: Float,
    liveText: String,
    parsedOperations: List<ParsedOperationItem>,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onOpenManualAdd: () -> Unit,
    onRmsChanged: (Float) -> Unit,
    onStatusChanged: (Boolean) -> Unit,
    onPartialText: (String) -> Unit,
    onFinalText: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onSaveBatch: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Audio permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onStartVoice()
        } else {
            onError("Разрешение на запись микрофона необходимо для распознавания речи")
        }
    }

    fun requestVoiceSession() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onStartVoice()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Lifecycle manager for speech recognition
    val speechManager = remember {
        SpeechRecognitionManager(
            context = context,
            onRmsChanged = onRmsChanged,
            onPartialText = onPartialText,
            onFinalText = onFinalText,
            onStatusChanged = onStatusChanged,
            onError = onError
        )
    }

    DisposableEffect(isVoiceActive) {
        if (isVoiceActive) {
            speechManager.startListening()
        } else {
            speechManager.stopListening()
        }
        onDispose {
            speechManager.stopListening()
        }
    }

    // Continuous flowing neon border gradient
    val infiniteBorderTransition = rememberInfiniteTransition(label = "InfiniteBorderFlow")
    val borderPhase by infiniteBorderTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "borderPhase"
    )

    val neonBorderBrush = Brush.sweepGradient(
        listOf(
            NeonEmerald,
            NeonIndigo,
            NeonCyan,
            NeonRose,
            NeonEmerald
        )
    )

    AnimatedContent(
        targetState = isVoiceActive,
        transitionSpec = {
            fadeIn(animationSpec = tween(320)) togetherWith fadeOut(animationSpec = tween(220))
        },
        label = "FabToVoiceCapsuleMorph"
    ) { active ->
        if (!active) {
            // 1. Clean Circular Neon FAB (no micro-mic badge)
            Box(
                modifier = modifier
                    .padding(end = 20.dp, bottom = 20.dp)
                    .shadow(elevation = 12.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NeonIndigo, NeonEmerald)
                        )
                    )
                    .combinedClickable(
                        onClick = { onOpenManualAdd() },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            requestVoiceSession()
                        }
                    )
                    .padding(16.dp)
                    .testTag("add_transaction_fab"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction or Hold for Voice",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            // 2. Compact Voice Input Capsule with dynamic upward spring growth
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .testTag("voice_input_capsule"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SlateSurfaceDark
                ),
                border = BorderStroke(1.5.dp, neonBorderBrush),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Header Row: Mic with pulsating halo | Realtime Text + Glowing Bar | Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Mic Button with expanding pulsating neon halo
                        val haloAlpha by animateFloatAsState(
                            targetValue = if (isListening) (0.25f + rmsLevel * 0.7f).coerceIn(0f, 0.95f) else 0f,
                            animationSpec = spring(stiffness = 500f),
                            label = "haloAlpha"
                        )
                        val haloRadiusScale by animateFloatAsState(
                            targetValue = if (isListening) (1f + rmsLevel * 0.45f) else 1f,
                            animationSpec = spring(stiffness = 450f),
                            label = "haloRadius"
                        )

                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Pulsating glowing neon aura
                            Canvas(modifier = Modifier.size(48.dp)) {
                                if (isListening && haloAlpha > 0f) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                NeonCyan.copy(alpha = haloAlpha),
                                                NeonEmerald.copy(alpha = haloAlpha * 0.6f),
                                                Color.Transparent
                                            )
                                        ),
                                        radius = (size.minDimension / 2f) * haloRadiusScale,
                                        center = Offset(size.width / 2f, size.height / 2f)
                                    )
                                }
                            }

                            // Clickable mic toggle (pause / resume)
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    speechManager.toggleListening()
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isListening) NeonIndigo.copy(alpha = 0.25f) else SlateCardDark
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        color = if (isListening) NeonEmerald else SlateBorderDark,
                                        shape = CircleShape
                                    )
                                    .testTag("toggle_voice_mic_button")
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = if (isListening) "Pause Listening" else "Resume Listening",
                                    tint = if (isListening) NeonEmerald else TextMutedDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Center: Live transcribed text & Moving Neon Glow Visualizer
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 2.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (liveText.isNotBlank()) liveText else if (isListening) "Слушаю вас..." else "Запись на паузе",
                                color = if (liveText.isNotBlank()) NeonCyan else TextPrimaryDark,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 10.dp Moving Neon Glow Visualizer
                            MovingNeonGlowVisualizer(
                                voiceLevel = rmsLevel,
                                isListening = isListening,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Right: Compact Close Button
                        IconButton(
                            onClick = { onStopVoice() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Voice",
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Dynamically Expanding List of Parsed Operations
                    if (parsedOperations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ГОТОВО К ДОБАВЛЕНИЮ (${parsedOperations.size})",
                                color = NeonEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(parsedOperations, key = { it.id }) { item ->
                                ParsedOperationCard(
                                    item = item,
                                    onDelete = { onRemoveItem(item.id) }
                                )
                            }
                        }

                        // Big Save Button
                        val totalAmount = parsedOperations.sumOf { it.amount }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onSaveBatch() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_voice_batch_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(NeonEmerald, NeonIndigo)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Сохранить (${parsedOperations.size}) • ${formatMoney(totalAmount)} ₽",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParsedOperationCard(
    item: ParsedOperationItem,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SlateCardDark,
        border = BorderStroke(1.dp, SlateBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                CategoryIconBadge(
                    iconName = item.categoryIcon,
                    colorHex = item.categoryColorHex,
                    size = 34.dp,
                    iconSize = 18.dp
                )

                Column {
                    Text(
                        text = item.title,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.categoryName,
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isIncome = item.type == TransactionType.INCOME
                val prefix = if (isIncome) "+ " else "- "
                val amountColor = if (isIncome) IncomeGreen else ExpenseRed

                Text(
                    text = "$prefix${formatMoney(item.amount)} ₽",
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Operation",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatMoney(value: Double): String {
    return String.format(Locale.US, "%,.0f", value).replace(",", " ")
}
