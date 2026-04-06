package com.app.radiosatelital.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    uiState: RadioUiState,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDataSaverToggle: (Boolean) -> Unit,
) {
    val station = uiState.selectedStation ?: return
    val backgroundColor = MaterialTheme.colorScheme.background
    val contentColor = MaterialTheme.colorScheme.onBackground
    val accentColor = MaterialTheme.colorScheme.primary
    val isLoading = uiState.playbackState is RadioPlaybackState.Loading
    val isError = uiState.playbackState is RadioPlaybackState.Error
    val statusLabel = when (uiState.playbackState) {
        is RadioPlaybackState.Playing -> "En vivo"
        is RadioPlaybackState.Loading -> "Conectando..."
        is RadioPlaybackState.Error -> "Error"
        is RadioPlaybackState.Paused -> "Pausado"
        is RadioPlaybackState.Selected -> "Lista"
        is RadioPlaybackState.NoneSelected -> "Sin radio"
    }
    var stationChangeMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(station.url) {
        stationChangeMessage = "Reproduciendo: ${station.name}"
        delay(1800)
        stationChangeMessage = null
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = contentColor)
                }
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = contentColor,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Radio, contentDescription = null, tint = contentColor, modifier = Modifier.size(90.dp))
                    }
                }
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = station.locationLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor.copy(alpha = 0.92f),
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isError) MaterialTheme.colorScheme.error else accentColor,
                    fontWeight = FontWeight.SemiBold,
                )

                if (!stationChangeMessage.isNullOrBlank()) {
                    Text(
                        text = stationChangeMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                    )
                }

                val nowPlayingText = when {
                    !uiState.nowPlayingArtist.isNullOrBlank() && !uiState.nowPlayingTitle.isNullOrBlank() -> {
                        "${uiState.nowPlayingArtist} · ${uiState.nowPlayingTitle}"
                    }
                    !uiState.nowPlayingTitle.isNullOrBlank() -> uiState.nowPlayingTitle
                    else -> "Sin informacion de la cancion actual"
                }
                Text(
                    text = nowPlayingText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor.copy(alpha = 0.92f),
                )

                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Conectando stream...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (isError) {
                    Text(
                        text = uiState.errorMessage ?: "No se pudo reproducir la emisora",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Ahorro de datos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(
                        checked = uiState.dataSaverMode,
                        onCheckedChange = onDataSaverToggle,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onPrevious, enabled = !isLoading) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Radio anterior", tint = contentColor)
                    }
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(74.dp)) {
                        Icon(
                            if (uiState.playbackState is RadioPlaybackState.Playing) {
                                Icons.Filled.PauseCircle
                            } else {
                                Icons.Filled.PlayCircle
                            },
                            contentDescription = if (uiState.playbackState is RadioPlaybackState.Playing) {
                                "Pausar reproducción"
                            } else {
                                "Reanudar reproducción"
                            },
                            tint = contentColor,
                            modifier = Modifier.size(62.dp),
                        )
                    }
                    IconButton(onClick = onNext, enabled = !isLoading) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Radio siguiente", tint = contentColor)
                    }
                }

                Slider(
                    value = uiState.volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Volumen ${(uiState.volume * 100f).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
