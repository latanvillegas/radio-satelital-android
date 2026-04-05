package com.app.radiosatelital.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val label: String) {
    PureWhite("Blanco puro"),
    PureBlackAmoled("Negro puro AMOLED"),
}

private val PureWhiteScheme = lightColorScheme(
    primary = Color(0xFF0E5A8A),
    onPrimary = Color.White,
    secondary = Color(0xFF3A6A8F),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F5F8),
    onBackground = Color(0xFF0D141C),
    onSurface = Color(0xFF0D141C),
)

private val PureBlackAmoledScheme = darkColorScheme(
    primary = Color(0xFF9FD0FF),
    onPrimary = Color(0xFF003352),
    secondary = Color(0xFFB5CBE6),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF0A0A0A),
    onBackground = Color(0xFFF2F2F2),
    onSurface = Color(0xFFF2F2F2),
)

@Composable
fun RadioSatelitalTheme(themeMode: AppThemeMode, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = when (themeMode) {
            AppThemeMode.PureWhite -> PureWhiteScheme
            AppThemeMode.PureBlackAmoled -> PureBlackAmoledScheme
        },
        content = content,
    )
}
