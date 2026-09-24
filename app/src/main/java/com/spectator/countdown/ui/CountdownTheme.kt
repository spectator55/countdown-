package com.spectator.countdown.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF205D59),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F4E8),
    onPrimaryContainer = Color(0xFF123D39),
    secondary = Color(0xFF715737),
    secondaryContainer = Color(0xFFFFE8CA),
    background = Color(0xFFF6F8F5),
    onBackground = Color(0xFF172927),
    surface = Color.White,
    onSurface = Color(0xFF172927),
    surfaceVariant = Color(0xFFE7EEEA),
    outlineVariant = Color(0xFFD4E0D9)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFABE3D1),
    onPrimary = Color(0xFF093B35),
    primaryContainer = Color(0xFF194D46),
    onPrimaryContainer = Color(0xFFD4F4E8),
    secondary = Color(0xFFE5C79F),
    secondaryContainer = Color(0xFF53452F),
    background = Color(0xFF101C1B),
    onBackground = Color(0xFFE1EDE7),
    surface = Color(0xFF1B2927),
    onSurface = Color(0xFFE1EDE7),
    surfaceVariant = Color(0xFF32413D),
    outlineVariant = Color(0xFF455C54)
)

@Composable
fun CountdownTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content)
}
