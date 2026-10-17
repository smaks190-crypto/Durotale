package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.GameMode
import com.example.ui.components.HintsDialog
import com.example.ui.components.RetroCanvasView
import com.example.ui.components.RetroControls
import com.example.ui.components.RetroDialogueBox
import com.example.ui.components.RetroHUD
import com.example.ui.components.UndertaleBattleView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RetroBlack
import com.example.viewmodel.DurotaleViewModel
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val viewModel: DurotaleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = RetroBlack
                ) { innerPadding ->
                    DurotaleGameScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> {
                viewModel.inputUp = true
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> {
                viewModel.inputDown = true
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                viewModel.inputLeft = true
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                viewModel.inputRight = true
                return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_SPACE -> {
                viewModel.handleConfirm()
                return true
            }
            KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                viewModel.handleCancel()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> {
                viewModel.inputUp = false
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> {
                viewModel.inputDown = false
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                viewModel.inputLeft = false
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                viewModel.inputRight = false
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
fun DurotaleGameScreen(
    viewModel: DurotaleViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // Screen Shake effect
    val shakeOffset = if (state.screenShakeTick > 0) {
        IntOffset(
            x = Random.nextInt(-8, 9),
            y = Random.nextInt(-8, 9)
        )
    } else {
        IntOffset.Zero
    }

    // Drugged wave transition for Secret Ending «Овощ»
    val infiniteTransition = rememberInfiniteTransition(label = "drugged_anim")
    val druggedWobble by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RetroBlack)
            .offset { shakeOffset }
            .then(if (state.isDruggedMode) Modifier.blur((druggedWobble * 0.3f).dp) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top HUD
            RetroHUD(
                playerHP = state.playerHP,
                maxHP = state.maxHP,
                endingsCount = state.endingsUnlocked.size,
                isMuted = state.isMuted,
                doorCombo = state.doorActionHistory,
                onToggleMute = { viewModel.toggleMute() },
                onResetCell = { viewModel.skipToCell() }
            )

            // Main Visual View: Overworld Canvas or Undertale Battle Screen
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.mode == GameMode.BATTLE) {
                    UndertaleBattleView(
                        battle = state.battle,
                        tick = state.tick,
                        onSoulPositionChanged = { x, y -> viewModel.setSoulPosition(x, y) }
                    )
                } else {
                    RetroCanvasView(
                        mode = state.mode,
                        player = state.player,
                        orderly = state.orderly,
                        orderly2 = state.orderly2,
                        orderly2Visible = state.orderly2Visible,
                        doorOpen = state.doorOpen,
                        orderlyAtWindow = state.orderlyAtWindow,
                        orderlyInside = state.orderlyInside,
                        isVegetable = state.isVegetable,
                        interactPrompt = state.interactPrompt,
                        tick = state.tick
                    )
                }
            }

            // Dialogue & Choice Box
            RetroDialogueBox(
                speaker = state.currentSpeaker,
                portrait = state.currentPortrait,
                dialogueText = state.displayedDialogueText,
                choices = state.choices,
                selectedChoiceIndex = state.selectedChoiceIndex,
                canAdvanceDialogue = state.canAdvanceDialogue,
                onChoiceSelected = { idx -> viewModel.selectChoiceByIndex(idx) },
                onAdvanceClick = { viewModel.handleConfirm() }
            )

            // Bottom On-Screen Controls
            RetroControls(
                onUpChange = { viewModel.inputUp = it },
                onDownChange = { viewModel.inputDown = it },
                onLeftChange = { viewModel.inputLeft = it },
                onRightChange = { viewModel.inputRight = it },
                onConfirm = { viewModel.handleConfirm() },
                onCancel = { viewModel.handleCancel() },
                onToggleHints = { viewModel.toggleHintsModal() }
            )
        }

        // Retro CRT scanline lines overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("crt_overlay")
        ) {
            val step = 4.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color(0x18000000),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5f
                )
                y += step
            }
        }

        // Hints Dialog
        if (state.showHintsModal) {
            HintsDialog(
                onDismiss = { viewModel.toggleHintsModal() },
                onRestartIntro = { viewModel.restartFullGame() }
            )
        }
    }
}
