package dev.tapscript.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB9A7FF),
    onPrimary = Color(0xFF24164D),
    secondary = Color(0xFF72D7C6),
    tertiary = Color(0xFFFFC66D),
    surface = Color(0xFF121318),
    surfaceVariant = Color(0xFF20222A),
    background = Color(0xFF0D0E12),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5A43B5),
    secondary = Color(0xFF006B5E),
    tertiary = Color(0xFF805600),
)

@Composable
fun TapScriptTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
