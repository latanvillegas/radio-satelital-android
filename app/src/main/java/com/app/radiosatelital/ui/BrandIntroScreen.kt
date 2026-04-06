package com.app.radiosatelital.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.delay

@Composable
fun BrandIntroScreen(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val iconScale = remember { Animatable(0.84f) }
    val iconAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleLetterSpacing = remember { Animatable(0.32f) }
    var showGlow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        playBrandSound(context)

        iconAlpha.animateTo(1f, animationSpec = tween(durationMillis = 240))
        iconScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        )

        titleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 260))
        titleLetterSpacing.animateTo(
            targetValue = 0.08f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        )

        showGlow = true
        delay(220)
        onFinished()
    }

    val background = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0B2545),
            Color(0xFF13315C),
            Color(0xFF134074),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .scale(iconScale.value)
                    .alpha(iconAlpha.value)
                    .background(
                        color = if (showGlow) Color(0x332EC4B6) else Color(0x223A86FF),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Radio,
                    contentDescription = null,
                    tint = Color(0xFFF1FAFF),
                    modifier = Modifier.size(72.dp),
                )
            }

            Text(
                text = "RADIO SATELITAL",
                modifier = Modifier.alpha(titleAlpha.value),
                style = TextStyle(
                    color = Color(0xFFF1FAFF),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = titleLetterSpacing.value.em,
                ),
            )
        }
    }
}

private suspend fun playBrandSound(context: Context) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

    val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 68)
    runCatching {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        delay(90)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
        delay(120)
    }
    tone.release()
}
