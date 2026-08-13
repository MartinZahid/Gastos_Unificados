package com.example.gastos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0E0F0C)
val CardBackground = Color(0xFF191A13)
val CardElevated = Color(0xFF212219)
val BorderLine = Color(0xFF2E2F23)
val TextPrimary = Color(0xFFF2F3EA)
val TextSecondary = Color(0xFF8B8D7E)
val Ink = Color(0xFF0E0F0C)
val Volt = Color(0xFFE9FF5B)
val Coral = Color(0xFFFF6B57)

private val DarkColors = darkColorScheme(
    primary = Volt,
    background = DarkBackground,
    surface = CardBackground,
    surfaceVariant = CardElevated,
    onPrimary = Ink,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun GastosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}