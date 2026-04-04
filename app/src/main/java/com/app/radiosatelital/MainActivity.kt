package com.app.radiosatelital

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
            RadioSatelitalApp()
        }
    }

    private companion object {
        const val TAG_STARTUP = "RADIO_STARTUP"
    }
}

@Composable
fun RadioSatelitalApp() {
    val context = LocalContext.current
    val playbackCoordinator = remember { PlaybackCoordinator(context.applicationContext) }

    DisposableEffect(Unit) {
        Log.i("RADIO_STARTUP", "UI lista: conexión al servicio en modo lazy")
        onDispose { playbackCoordinator.release() }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Radio Satelital",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    val uiState = playbackCoordinator.uiState
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(text = "Estado", style = MaterialTheme.typography.titleMedium)
                            Text(text = uiState.statusText, style = MaterialTheme.typography.bodyLarge)
                            uiState.selectedStation?.let { station ->
                                Text(
                                    text = "Seleccionada: ${station.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(text = station.locationLabel, style = MaterialTheme.typography.bodySmall)
                            }
                            if (uiState.errorMessage != null) {
                                Text(text = uiState.errorMessage, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { playbackCoordinator.togglePlayback() },
                                    enabled = uiState.selectedStation != null,
                                ) {
                                    Text(
                                        text = if (uiState.playbackState is RadioPlaybackState.Playing) {
                                            "Pausar"
                                        } else {
                                            "Reproducir"
                                        },
                                    )
                                }
                                OutlinedButton(
                                    onClick = { playbackCoordinator.stop() },
                                    enabled = uiState.selectedStation != null,
                                ) {
                                    Text(text = "Detener")
                                }
                            }
                        }
                    }

                    Text(text = "Emisoras", style = MaterialTheme.typography.titleLarge)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(defaultStations) { station ->
                            val selected = playbackCoordinator.uiState.selectedStation?.url == station.url
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { playbackCoordinator.play(station) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(text = station.name, style = MaterialTheme.typography.titleMedium)
                                    Text(text = station.locationLabel, style = MaterialTheme.typography.bodySmall)
                                    if (selected) {
                                        Text(text = "Seleccionada", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
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
) {
    val statusText: String
        get() = when (playbackState) {
            RadioPlaybackState.NoneSelected -> "Ninguna emisora seleccionada"
            RadioPlaybackState.Selected -> "Emisora seleccionada"
            RadioPlaybackState.Loading -> "Cargando reproducción"
            RadioPlaybackState.Playing -> "Reproduciendo"
            RadioPlaybackState.Paused -> "Pausada"
            is RadioPlaybackState.Error -> "Error"
        }
}

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

    private fun playInternal(station: RadioStation) {
        val activeController = controller ?: return
        pendingStation = null
        val mediaItem = MediaItem.Builder()
            .setUri(station.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.locationLabel)
                    .build(),
            )
            .build()

        try {
            Log.i(TAG_STARTUP, "playInternal(): preparando reproducción url=${station.url}")
            activeController.setMediaItem(mediaItem)
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
        val selectedStation = uiState.selectedStation
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
