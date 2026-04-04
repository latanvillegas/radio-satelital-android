package com.app.radiosatelital

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG_STARTUP, "MainActivity.onCreate: inicio de app")
        setContent {
            RadioSatelitalTheme {
                RadioSatelitalApp()
            }
        }
    }

    private companion object {
        const val TAG_STARTUP = "RADIO_STARTUP"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioSatelitalApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val playbackCoordinator = remember { PlaybackCoordinator(context.applicationContext) }

    DisposableEffect(Unit) {
        Log.i("RADIO_STARTUP", "UI lista: conexión al servicio en modo lazy")
        onDispose { playbackCoordinator.release() }
    }

    val uiState = playbackCoordinator.uiState

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    HeaderSection(isPlaying = uiState.playbackState is RadioPlaybackState.Playing)
                }

                item {
                    NowPlayingCard(
                        uiState = uiState,
                        onPlayPause = playbackCoordinator::togglePlayback,
                        onStop = playbackCoordinator::stop,
                        onPrevious = playbackCoordinator::previous,
                        onNext = playbackCoordinator::next,
                    )
                }

                item {
                    Text(
                        text = "Emisoras",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                items(defaultStations) { station ->
                    val selected = uiState.selectedStation?.url == station.url
                    StationRow(
                        station = station,
                        selected = selected,
                        isPlaying = selected && uiState.playbackState is RadioPlaybackState.Playing,
                        onClick = { playbackCoordinator.play(station) },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(isPlaying: Boolean) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Radio Satelital",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Streaming en vivo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LivePill(isPlaying = isPlaying)
        }
    }
}

@Composable
private fun LivePill(isPlaying: Boolean) {
    val bg = if (isPlaying) Color(0xFFD32F2F) else MaterialTheme.colorScheme.outlineVariant
    val text = if (isPlaying) "LIVE" else "IDLE"

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun NowPlayingCard(
    uiState: RadioUiState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Equalizer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Crossfade(targetState = uiState.selectedStation, label = "station_crossfade") { station ->
                if (station == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Selecciona una emisora para comenzar",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "La reproducción continuará en segundo plano",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = station.locationLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(state = uiState.playbackState)
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.alpha(0.95f),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onPrevious, enabled = uiState.selectedStation != null) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Anterior")
                }
                FilledTonalButton(
                    onClick = onPlayPause,
                    enabled = uiState.selectedStation != null,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    val isPlaying = uiState.playbackState is RadioPlaybackState.Playing
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPlaying) "Pausar" else "Reproducir")
                }
                IconButton(onClick = onNext, enabled = uiState.selectedStation != null) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Siguiente")
                }
            }

            OutlinedButton(
                onClick = onStop,
                enabled = uiState.selectedStation != null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.StopCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Detener")
            }
        }
    }
}

@Composable
private fun StatusChip(state: RadioPlaybackState) {
    val label = when (state) {
        RadioPlaybackState.NoneSelected -> "Sin selección"
        RadioPlaybackState.Selected -> "Seleccionada"
        RadioPlaybackState.Loading -> "Conectando"
        RadioPlaybackState.Playing -> "Reproduciendo"
        RadioPlaybackState.Paused -> "Pausada"
        is RadioPlaybackState.Error -> "Error"
    }

    val icon = when (state) {
        RadioPlaybackState.Playing -> Icons.Filled.PlayCircle
        RadioPlaybackState.Paused -> Icons.Filled.PauseCircle
        is RadioPlaybackState.Error -> Icons.Filled.StopCircle
        else -> Icons.Filled.Radio
    }

    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
    )
}

@Composable
private fun StationRow(
    station: RadioStation,
    selected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Equalizer else Icons.Filled.Radio,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = station.country,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (station.region.isNotBlank()) {
                        Text(
                            text = "• ${station.region}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Seleccionada",
                    tint = if (isPlaying) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private val RadioStation.locationLabel: String
    get() = if (region.isBlank()) country else "$country · $region"

private sealed interface RadioPlaybackState {
    data object NoneSelected : RadioPlaybackState
    data object Selected : RadioPlaybackState
    data object Loading : RadioPlaybackState
    data object Playing : RadioPlaybackState
    data object Paused : RadioPlaybackState
    data class Error(val message: String) : RadioPlaybackState
}

private data class RadioUiState(
    val selectedStation: RadioStation? = null,
    val playbackState: RadioPlaybackState = RadioPlaybackState.NoneSelected,
    val errorMessage: String? = null,
)

private class PlaybackCoordinator(private val appContext: android.content.Context) {

    var uiState by mutableStateOf(RadioUiState())
        private set

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingStation: RadioStation? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updateStateFromController()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            updateStateFromController()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateStateFromController()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateStateFromController()
        }

        override fun onPlayerError(error: PlaybackException) {
            uiState = uiState.copy(
                playbackState = RadioPlaybackState.Error(
                    error.message ?: "No se pudo reproducir la emisora",
                ),
                errorMessage = error.message ?: "No se pudo reproducir la emisora",
            )
        }
    }

    fun connect() {
        if (controllerFuture != null) {
            return
        }

        Log.i(TAG_STARTUP, "connect(): creando MediaController")

        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    controller = future.get().also { it.addListener(listener) }
                    Log.i(TAG_STARTUP, "connect(): MediaController conectado")
                    updateStateFromController()
                    pendingStation?.let { playInternal(it) }
                } catch (_: Exception) {
                    Log.e(TAG_STARTUP, "connect(): fallo al conectar MediaController")
                    uiState = uiState.copy(
                        playbackState = RadioPlaybackState.Error(
                            "No se pudo conectar al servicio de reproducción",
                        ),
                        errorMessage = "No se pudo conectar al servicio de reproducción",
                    )
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    fun release() {
        Log.i(TAG_STARTUP, "release(): liberando MediaController")
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        pendingStation = null
    }

    fun play(station: RadioStation) {
        ensureConnected()
        Log.i(TAG_STARTUP, "play(): emisora seleccionada=${station.name}")
        uiState = uiState.copy(
            selectedStation = station,
            playbackState = RadioPlaybackState.Loading,
            errorMessage = null,
        )
        pendingStation = station
        if (controller == null) {
            connect()
            return
        }
        playInternal(station)
    }

    fun togglePlayback() {
        ensureConnected()
        val activeController = controller ?: return
        when {
            activeController.isPlaying -> activeController.pause()
            uiState.selectedStation != null -> activeController.play()
        }
    }

    fun stop() {
        ensureConnected()
        controller?.stop()
        uiState = uiState.copy(
            playbackState = if (uiState.selectedStation == null) {
                RadioPlaybackState.NoneSelected
            } else {
                RadioPlaybackState.Selected
            },
        )
    }

    fun previous() {
        ensureConnected()
        controller?.seekToPreviousMediaItem()
    }

    fun next() {
        ensureConnected()
        controller?.seekToNextMediaItem()
    }

    private fun playInternal(station: RadioStation) {
        val activeController = controller ?: return
        pendingStation = null
        val startIndex = defaultStations.indexOfFirst { it.url == station.url }
        if (startIndex == -1) {
            uiState = uiState.copy(
                playbackState = RadioPlaybackState.Error("No se encontró la emisora seleccionada"),
                errorMessage = "No se encontró la emisora seleccionada",
            )
            return
        }
        val mediaItems = defaultStations.map { it.toMediaItem() }

        try {
            Log.i(TAG_STARTUP, "playInternal(): preparando reproducción url=${station.url}")
            activeController.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            activeController.prepare()
            activeController.play()
            updateStateFromController()
        } catch (e: Exception) {
            Log.e(TAG_STARTUP, "playInternal(): excepción al reproducir", e)
            uiState = uiState.copy(
                playbackState = RadioPlaybackState.Error(
                    e.message ?: "No se pudo reproducir la emisora",
                ),
                errorMessage = e.message ?: "No se pudo reproducir la emisora",
            )
        }
    }

    private fun updateStateFromController() {
        val activeController = controller ?: return
        val currentMediaId = activeController.currentMediaItem?.mediaId
        val selectedStation = currentMediaId?.let { mediaId ->
            defaultStations.firstOrNull { station -> station.url == mediaId }
        } ?: uiState.selectedStation

        val newState = when {
            activeController.playerError != null -> RadioPlaybackState.Error(
                activeController.playerError?.message ?: "No se pudo reproducir la emisora",
            )
            activeController.isPlaying -> RadioPlaybackState.Playing
            activeController.playbackState == Player.STATE_BUFFERING -> RadioPlaybackState.Loading
            selectedStation != null -> {
                if (activeController.playbackState == Player.STATE_READY) {
                    RadioPlaybackState.Paused
                } else {
                    RadioPlaybackState.Selected
                }
            }
            else -> RadioPlaybackState.NoneSelected
        }

        uiState = uiState.copy(
            selectedStation = selectedStation,
            playbackState = newState,
            errorMessage = if (newState is RadioPlaybackState.Error) newState.message else null,
        )
    }

    private fun ensureConnected() {
        if (controller == null && controllerFuture == null) {
            connect()
        }
    }

    private companion object {
        const val TAG_STARTUP = "RADIO_STARTUP"
    }
}

private fun RadioStation.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(url)
        .setUri(url)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
                .setArtist(locationLabel)
                .build(),
        )
        .build()
}

@Composable
private fun RadioSatelitalTheme(content: @Composable () -> Unit) {
    val light = lightColorScheme(
        primary = Color(0xFF0F5C6E),
        onPrimary = Color.White,
        secondary = Color(0xFF2E6F52),
        tertiary = Color(0xFFA55D00),
        background = Color(0xFFF4F7F9),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE5EDF2),
        error = Color(0xFFBA1A1A),
    )

    val dark = darkColorScheme(
        primary = Color(0xFF86D1E6),
        onPrimary = Color(0xFF003640),
        secondary = Color(0xFF8ED7B5),
        tertiary = Color(0xFFFFB870),
        background = Color(0xFF0E1418),
        surface = Color(0xFF121A1F),
        surfaceVariant = Color(0xFF24313A),
        error = Color(0xFFFFB4AB),
    )

    val scheme = if (androidx.compose.foundation.isSystemInDarkTheme()) dark else light
    MaterialTheme(colorScheme = scheme, content = content)
}
