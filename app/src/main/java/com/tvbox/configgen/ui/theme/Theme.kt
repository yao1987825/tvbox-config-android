package com.tvbox.configgen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2C3E50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E8EE),
    secondary = Color(0xFF3B82F6),
    background = Color(0xFFF7F7F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFFAFBFC),
    error = Color(0xFFEF4444)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4A90E2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A2733),
    secondary = Color(0xFF6BA3EB),
    background = Color(0xFF0F1419),
    surface = Color(0xFF1A1F2E),
    surfaceVariant = Color(0xFF232938),
    error = Color(0xFFF87171)
)

@Composable
fun TvBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}