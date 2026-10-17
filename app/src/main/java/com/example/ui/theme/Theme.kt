package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RetroColorScheme = darkColorScheme(
    primary = RetroYellow,
    onPrimary = RetroBoxBlack,
    secondary = RetroRed,
    onSecondary = RetroWhite,
    tertiary = RetroGreen,
    onTertiary = RetroBoxBlack,
    background = RetroBlack,
    onBackground = RetroWhite,
    surface = RetroBoxBlack,
    onSurface = RetroWhite,
    surfaceVariant = RetroDarkGray,
    onSurfaceVariant = RetroWhite,
    outline = RetroWhite
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RetroColorScheme,
        typography = Typography,
        content = content
    )
}
