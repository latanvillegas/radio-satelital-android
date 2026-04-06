package com.app.radiosatelital.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val label: String) {
    PureWhite("Blanco puro"),
    PureBlackAmoled("Negro puro AMOLED"),
    PureBlue("Azul puro"),
    IslandGlass("Island Glass"),
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

private val PureBlueScheme = darkColorScheme(
    primary = Color(0xFF6CB8FF),
    onPrimary = Color(0xFF002847),
    secondary = Color(0xFFAED4FF),
    background = Color(0xFF001A33),
    surface = Color(0xFF001A33),
    surfaceVariant = Color(0xFF0A2846),
    onBackground = Color(0xFFEAF3FF),
    onSurface = Color(0xFFEAF3FF),
)

private val IslandGlassScheme = lightColorScheme(
    primary = Color(0xFF0A6E8A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF1E88A8),
    background = Color(0xFFF4FBFF),
    surface = Color(0xFFEAF7FF),
    surfaceVariant = Color(0xFFDCEFFD),
    onBackground = Color(0xFF0B1E2A),
    onSurface = Color(0xFF0B1E2A),
)

@Composable
fun RadioSatelitalTheme(themeMode: AppThemeMode, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = when (themeMode) {
            AppThemeMode.PureWhite -> PureWhiteScheme
            AppThemeMode.PureBlackAmoled -> PureBlackAmoledScheme
            AppThemeMode.PureBlue -> PureBlueScheme
            AppThemeMode.IslandGlass -> IslandGlassScheme
        },
        content = content,
    )
}
