package com.app.radiosatelital

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.app.radiosatelital.ui.BrandIntroScreen
import com.app.radiosatelital.ui.MainScreen
import com.app.radiosatelital.ui.RadioCardSizeMode
import com.app.radiosatelital.ui.RadioLayoutMode
import com.app.radiosatelital.ui.rememberPlaybackCoordinator
import com.app.radiosatelital.ui.theme.AppThemeMode
import com.app.radiosatelital.ui.theme.RadioSatelitalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.i(TAG_STARTUP, "MainActivity.onCreate: inicio de app")

        val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedTheme = preferences.getString(KEY_THEME_MODE, AppThemeMode.PureWhite.name)
            ?.let { value -> enumValueOrDefault<AppThemeMode>(value, AppThemeMode.PureWhite) }
            ?: AppThemeMode.PureWhite
        val savedLayout = preferences.getString(KEY_LAYOUT_MODE, RadioLayoutMode.OneRow.name)
            ?.let { value -> enumValueOrDefault<RadioLayoutMode>(value, RadioLayoutMode.OneRow) }
            ?: RadioLayoutMode.OneRow
        val savedCardSize = preferences.getString(KEY_CARD_SIZE_MODE, RadioCardSizeMode.Normal.name)
            ?.let { value -> enumValueOrDefault<RadioCardSizeMode>(value, RadioCardSizeMode.Normal) }
            ?: RadioCardSizeMode.Normal
        val savedAnimationsEnabled = preferences.getBoolean(KEY_ANIMATIONS_ENABLED, true)
        val hasSeenIntro = preferences.getBoolean(KEY_INTRO_SEEN, false)

        setContent {
            var introFinished by remember { mutableStateOf(hasSeenIntro) }

            if (!introFinished) {
                BrandIntroScreen(
                    onFinished = {
                        introFinished = true
                        preferences.edit().putBoolean(KEY_INTRO_SEEN, true).apply()
                    },
                )
                return@setContent
            }

            var themeMode by remember { mutableStateOf(savedTheme) }
            var layoutMode by remember { mutableStateOf(savedLayout) }
            var cardSizeMode by remember { mutableStateOf(savedCardSize) }
            var animationsEnabled by remember { mutableStateOf(savedAnimationsEnabled) }

            RadioSatelitalTheme(themeMode = themeMode) {
                val coordinator = rememberPlaybackCoordinator()
                MainScreen(
                    coordinator = coordinator,
                    themeMode = themeMode,
                    layoutMode = layoutMode,
                    cardSizeMode = cardSizeMode,
                    animationsEnabled = animationsEnabled,
                    onThemeChange = {
                        themeMode = it
                        preferences.edit().putString(KEY_THEME_MODE, it.name).apply()
                    },
                    onLayoutModeChange = {
                        layoutMode = it
                        preferences.edit().putString(KEY_LAYOUT_MODE, it.name).apply()
                    },
                    onCardSizeModeChange = {
                        cardSizeMode = it
                        preferences.edit().putString(KEY_CARD_SIZE_MODE, it.name).apply()
                    },
                    onAnimationsEnabledChange = {
                        animationsEnabled = it
                        preferences.edit().putBoolean(KEY_ANIMATIONS_ENABLED, it).apply()
                    },
                    onResetAppearance = {
                        themeMode = AppThemeMode.PureWhite
                        layoutMode = RadioLayoutMode.OneRow
                        cardSizeMode = RadioCardSizeMode.Normal
                        animationsEnabled = true
                        preferences.edit()
                            .putString(KEY_THEME_MODE, AppThemeMode.PureWhite.name)
                            .putString(KEY_LAYOUT_MODE, RadioLayoutMode.OneRow.name)
                            .putString(KEY_CARD_SIZE_MODE, RadioCardSizeMode.Normal.name)
                            .putBoolean(KEY_ANIMATIONS_ENABLED, true)
                            .apply()
                    },
                )
            }
        }
    }

    private companion object {
        const val TAG_STARTUP = "RADIO_STARTUP"
        const val PREFS_NAME = "radio_satelital_prefs"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LAYOUT_MODE = "layout_mode"
        const val KEY_CARD_SIZE_MODE = "card_size_mode"
        const val KEY_ANIMATIONS_ENABLED = "animations_enabled"
        const val KEY_INTRO_SEEN = "intro_seen"
    }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T {
    return runCatching { enumValueOf<T>(value) }.getOrElse { fallback }
}
