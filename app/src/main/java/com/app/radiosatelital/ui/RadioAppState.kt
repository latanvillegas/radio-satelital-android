package com.app.radiosatelital.ui

import android.content.ComponentName
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.app.radiosatelital.PlaybackService
import com.app.radiosatelital.RadioStation
import com.app.radiosatelital.defaultStations
import com.google.common.util.concurrent.ListenableFuture

enum class HomeTab(val label: String) {
    Music("Musica"),
    Favorites("Favoritos"),
    Country("País"),
    Mine("Mis radios"),
}

sealed interface RadioPlaybackState {
    data object NoneSelected : RadioPlaybackState
    data object Selected : RadioPlaybackState
    data object Loading : RadioPlaybackState
    data object Playing : RadioPlaybackState
    data object Paused : RadioPlaybackState
    data class Error(val message: String) : RadioPlaybackState
}

data class RadioUiState(
    val selectedStation: RadioStation? = null,
    val selectedStationId: String? = null,
    val playbackState: RadioPlaybackState = RadioPlaybackState.NoneSelected,
    val errorMessage: String? = null,
    val currentTab: HomeTab = HomeTab.Music,
    val volume: Float = 1f,
)

class PlaybackCoordinator(private val appContext: android.content.Context) {

    var uiState by mutableStateOf(RadioUiState())
        private set

    private data class PendingSelection(
        val index: Int,
        val station: RadioStation,
        val catalog: List<RadioStation>,
    )

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingSelection: PendingSelection? = null
    private var activeCatalog: List<RadioStation> = defaultStations

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

    fun setTab(tab: HomeTab) {
        uiState = uiState.copy(currentTab = tab)
    }

    fun connect() {
        if (controllerFuture != null) return

        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    controller = future.get().also { it.addListener(listener) }
                    updateStateFromController()
                    pendingSelection?.let { playInternal(it.index, it.station, it.catalog) }
                } catch (_: Exception) {
                    uiState = uiState.copy(
                        playbackState = RadioPlaybackState.Error(
                            "No se pudo conectar al servicio de reproduccion",
                        ),
                        errorMessage = "No se pudo conectar al servicio de reproduccion",
                    )
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    fun release() {
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        pendingSelection = null
    }

    fun play(index: Int, station: RadioStation, catalog: List<RadioStation> = defaultStations) {
        ensureConnected()
        Log.i("RADIO_UI", "click item: nombre=${station.name} index=$index url=${station.url}")
        uiState = uiState.copy(
            selectedStation = station,
            selectedStationId = index.toString(),
            playbackState = RadioPlaybackState.Loading,
            errorMessage = null,
        )
        Log.i("RADIO_UI", "selectedStation actualizado: nombre=${station.name} id=$index")
        activeCatalog = catalog
        pendingSelection = PendingSelection(index, station, catalog)
        if (controller == null) {
            connect()
            return
        }
        playInternal(index, station, catalog)
    }

    fun togglePlayback() {
        ensureConnected()
        val activeController = controller ?: return
        when {
            activeController.isPlaying -> activeController.pause()
            uiState.selectedStationId != null -> activeController.play()
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

    fun setVolume(value: Float) {
        val safe = value.coerceIn(0f, 1f)
        controller?.volume = safe
        uiState = uiState.copy(volume = safe)
    }

    private fun playInternal(index: Int, station: RadioStation, catalog: List<RadioStation>) {
        val activeController = controller ?: return
        pendingSelection = null
        if (index !in catalog.indices) {
            uiState = uiState.copy(
                playbackState = RadioPlaybackState.Error("No se encontro la emisora seleccionada"),
                errorMessage = "No se encontro la emisora seleccionada",
            )
            return
        }
        val mediaItems = catalog.mapIndexed { i, item -> item.toMediaItem(i) }

        try {
            Log.i(
                "RADIO_CTRL",
                "mediaItem enviado al controller: radio=${station.name} mediaId=$index url=${station.url}",
            )
            activeController.setMediaItems(mediaItems, index, C.TIME_UNSET)
            activeController.prepare()
            activeController.play()
            Log.i("RADIO_CTRL", "prepare/play ejecutados: mediaId=$index")
            updateStateFromController()
        } catch (e: Exception) {
            Log.e(
                "RADIO_CTRL",
                "playInternal(): excepcion al reproducir radio=${station.name} url=${station.url} msg=${e.message}",
                e,
            )
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
        val selectedStation = currentMediaId?.toIntOrNull()?.let { stationIndex ->
            activeCatalog.getOrNull(stationIndex)
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
            selectedStationId = currentMediaId ?: uiState.selectedStationId,
            playbackState = newState,
            errorMessage = if (newState is RadioPlaybackState.Error) newState.message else null,
            volume = activeController.volume,
        )
    }

    private fun ensureConnected() {
        if (controller == null && controllerFuture == null) {
            connect()
        }
    }
}

private fun RadioStation.toMediaItem(index: Int): MediaItem {
    return MediaItem.Builder()
        .setMediaId(index.toString())
        .setUri(url)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
                .setArtist(locationLabel)
                .build(),
        )
        .build()
}

val RadioStation.locationLabel: String
    get() = if (region.isBlank()) country else "$country · $region"

@Composable
fun rememberPlaybackCoordinator(): PlaybackCoordinator {
    val context = LocalContext.current
    val coordinator = remember { PlaybackCoordinator(context.applicationContext) }

    DisposableEffect(Unit) {
        onDispose { coordinator.release() }
    }

    return coordinator
}
