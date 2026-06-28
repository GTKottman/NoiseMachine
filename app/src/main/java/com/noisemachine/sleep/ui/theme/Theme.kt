package com.noisemachine.sleep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ForestDarkScheme = darkColorScheme(
    primary = Color(0xFF8CCB9B),
    secondary = Color(0xFF6AB89A),
    tertiary = Color(0xFF7FA5D6),
    background = Color(0xFF081014),
    surface = Color(0xFF0F1A1F),
    onPrimary = Color(0xFF102114),
    onBackground = Color(0xFFE7F2EE),
    onSurface = Color(0xFFD8E5DF)
)

@Composable
fun NoiseMachineTheme(
    content: @Composable () -> Unit
) {
    val useDark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (useDark) ForestDarkScheme else ForestDarkScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
