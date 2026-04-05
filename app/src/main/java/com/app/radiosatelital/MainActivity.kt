package com.app.radiosatelital

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.app.radiosatelital.ui.MainScreen
import com.app.radiosatelital.ui.RadioLayoutMode
import com.app.radiosatelital.ui.rememberPlaybackCoordinator
import com.app.radiosatelital.ui.theme.AppThemeMode
import com.app.radiosatelital.ui.theme.RadioSatelitalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    enableEdgeToEdge()
        Log.i(TAG_STARTUP, "MainActivity.onCreate: inicio de app")
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(AppThemeMode.PureWhite) }
            var layoutMode by rememberSaveable { mutableStateOf(RadioLayoutMode.OneRow) }
            RadioSatelitalTheme(themeMode = themeMode) {
                val coordinator = rememberPlaybackCoordinator()
                MainScreen(
                    coordinator = coordinator,
                    themeMode = themeMode,
                    layoutMode = layoutMode,
                    onThemeChange = { themeMode = it },
                    onLayoutModeChange = { layoutMode = it },
                )
            }
        }
    }

    private companion object {
        const val TAG_STARTUP = "RADIO_STARTUP"
    }
}
