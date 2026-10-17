package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RetroBoxBlack
import com.example.ui.theme.RetroOrange
import com.example.ui.theme.RetroRed
import com.example.ui.theme.RetroWhite
import com.example.ui.theme.RetroYellow

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DPadButton(
    label: String,
    testTag: String,
    onStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(46.dp)
            .testTag(testTag)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPressed) Color(0x66FFFF00) else Color(0x22FFFFFF))
            .border(
                2.dp,
                if (isPressed) RetroYellow else Color(0x66FFFFFF),
                RoundedCornerShape(6.dp)
            )
            .pointerInteropFilter { motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onStateChange(true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        onStateChange(false)
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) RetroYellow else RetroWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RetroControls(
    onUpChange: (Boolean) -> Unit,
    onDownChange: (Boolean) -> Unit,
    onLeftChange: (Boolean) -> Unit,
    onRightChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onToggleHints: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // D-Pad
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DPadButton(label = "◀", testTag = "dpad_left", onStateChange = onLeftChange)

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DPadButton(label = "▲", testTag = "dpad_up", onStateChange = onUpChange)
                DPadButton(label = "▼", testTag = "dpad_down", onStateChange = onDownChange)
            }

            DPadButton(label = "▶", testTag = "dpad_right", onStateChange = onRightChange)
        }

        // Center Hints Button
        Box(
            modifier = Modifier
                .testTag("btn_hints")
                .clip(RoundedCornerShape(4.dp))
                .background(RetroBoxBlack)
                .border(2.dp, RetroOrange, RoundedCornerShape(4.dp))
                .clickable { onToggleHints() }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "? ПАМЯТКА",
                color = RetroOrange,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Action Buttons: X & Z
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cancel / Skip Button (X)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_x")
                    .clip(CircleShape)
                    .background(Color(0x33FF1744))
                    .border(2.dp, RetroRed, CircleShape)
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "X",
                    color = RetroRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Confirm / Action Button (Z)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .testTag("btn_z")
                    .clip(CircleShape)
                    .background(Color(0x33FFFF00))
                    .border(3.dp, RetroYellow, CircleShape)
                    .clickable { onConfirm() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Z",
                    color = RetroYellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
