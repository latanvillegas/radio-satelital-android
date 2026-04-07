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
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.app.radiosatelital.PlaybackService
import com.app.radiosatelital.RadioStation
import com.app.radiosatelital.data.artwork.ArtworkRepository
import com.app.radiosatelital.data.firebase.RadioRepository
import com.app.radiosatelital.defaultStations
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URI

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
    val nowPlayingArtist: String? = null,
    val nowPlayingTitle: String? = null,
    val artworkUrl: String? = null,
    val dataSaverMode: Boolean = false,
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val artworkRepository = ArtworkRepository()
    private val radioRepository = RadioRepository(appContext)
    private var lastArtworkLookupKey: String? = null
    private var countedStationUrl: String? = null
    private var listenersHeartbeatJob: Job? = null

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

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            updateNowPlayingFromMetadata(mediaMetadata)
        }

        override fun onMetadata(metadata: androidx.media3.common.Metadata) {
            updateNowPlayingFromTimedMetadata(metadata)
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(
                "RADIO_CTRL",
                "onPlayerError: code=${error.errorCode} name=${error.errorCodeName} msg=${error.message} cause=${error.cause?.message} station=${uiState.selectedStation?.name} url=${uiState.selectedStation?.url}",
                error,
            )
            uiState = uiState.copy(
                playbackState = RadioPlaybackState.Error(mapPlaybackError(error)),
                errorMessage = mapPlaybackError(error),
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
        syncLiveListeners(null)
        listenersHeartbeatJob?.cancel()
        listenersHeartbeatJob = null
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        pendingSelection = null
        scope.cancel()
    }

    fun play(index: Int, station: RadioStation, catalog: List<RadioStation> = defaultStations) {
        ensureConnected()
        Log.i("RADIO_UI", "click item: nombre=${station.name} index=$index url=${station.url}")
        uiState = uiState.copy(
            selectedStation = station,
            selectedStationId = index.toString(),
            playbackState = RadioPlaybackState.Loading,
            errorMessage = null,
            nowPlayingArtist = null,
            nowPlayingTitle = null,
            artworkUrl = null,
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

    fun retryCurrentStation() {
        ensureConnected()
        val currentIndex = uiState.selectedStationId?.toIntOrNull()
        val currentStation = currentIndex?.let { activeCatalog.getOrNull(it) } ?: uiState.selectedStation

        if (currentIndex != null && currentStation != null) {
            playInternal(currentIndex, currentStation, activeCatalog)
        } else {
            controller?.play()
        }
    }

    fun setDataSaverMode(enabled: Boolean) {
        uiState = uiState.copy(
            dataSaverMode = enabled,
            artworkUrl = if (enabled) null else uiState.artworkUrl,
        )
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
        updateNowPlayingFromMetadata(activeController.mediaMetadata)

        val liveStation = if (newState is RadioPlaybackState.Playing) selectedStation?.url else null
        syncLiveListeners(liveStation)
    }

    private fun syncLiveListeners(targetStationUrl: String?) {
        if (targetStationUrl == countedStationUrl) return
        val previous = countedStationUrl
        countedStationUrl = targetStationUrl
        listenersHeartbeatJob?.cancel()
        listenersHeartbeatJob = null

        scope.launch {
            previous?.let { radioRepository.updateLiveListeners(it, -1) }
            targetStationUrl?.let { radioRepository.updateLiveListeners(it, +1) }
        }

        if (!targetStationUrl.isNullOrBlank()) {
            listenersHeartbeatJob = scope.launch(Dispatchers.IO) {
                while (isActive && countedStationUrl == targetStationUrl) {
                    radioRepository.updateLiveListeners(targetStationUrl, 0)
                    delay(25_000)
                }
            }
        }
    }

    private fun updateNowPlayingFromMetadata(metadata: MediaMetadata) {
        val parsed = StreamMetadataParser.parse(
            artistRaw = metadata.artist?.toString(),
            titleRaw = metadata.title?.toString(),
            displayTitleRaw = metadata.displayTitle?.toString(),
        )

        uiState = uiState.copy(
            nowPlayingArtist = parsed.artist,
            nowPlayingTitle = parsed.title,
        )

        if (uiState.dataSaverMode) {
            uiState = uiState.copy(artworkUrl = null)
            return
        }

        val lookupKey = listOf(parsed.artist, parsed.title).joinToString("||")
        if (lookupKey.isBlank() || lookupKey == lastArtworkLookupKey) return
        lastArtworkLookupKey = lookupKey

        scope.launch {
            val artwork = artworkRepository.fetchArtworkIfPossible(parsed.artist, parsed.title)
            uiState = uiState.copy(
                artworkUrl = artwork.artworkUrl,
            )
        }
    }

    private fun updateNowPlayingFromTimedMetadata(metadata: androidx.media3.common.Metadata) {
        var combinedTitle: String? = null

        for (i in 0 until metadata.length()) {
            val entry = metadata[i]
            if (entry is IcyInfo && !entry.title.isNullOrBlank()) {
                combinedTitle = entry.title
                break
            }
        }

        if (combinedTitle.isNullOrBlank()) return

        val parsed = StreamMetadataParser.parse(
            artistRaw = null,
            titleRaw = combinedTitle,
            displayTitleRaw = combinedTitle,
        )

        if (parsed.artist.isNullOrBlank() && parsed.title.isNullOrBlank()) return

        uiState = uiState.copy(
            nowPlayingArtist = parsed.artist,
            nowPlayingTitle = parsed.title,
        )

        if (uiState.dataSaverMode) {
            uiState = uiState.copy(artworkUrl = null)
            return
        }

        val lookupKey = listOf(parsed.artist, parsed.title).joinToString("||")
        if (lookupKey.isBlank() || lookupKey == lastArtworkLookupKey) return
        lastArtworkLookupKey = lookupKey

        scope.launch {
            val artwork = artworkRepository.fetchArtworkIfPossible(parsed.artist, parsed.title)
            uiState = uiState.copy(
                artworkUrl = artwork.artworkUrl,
            )
        }
    }

    private fun ensureConnected() {
        if (controller == null && controllerFuture == null) {
            connect()
        }
    }

    private fun mapPlaybackError(error: PlaybackException): String {
        val message = error.message.orEmpty().lowercase()
        return when {
            message.contains("unable to connect") || message.contains("network") -> {
                "Sin internet o conexion inestable"
            }
            message.contains("source") || message.contains("404") || message.contains("403") -> {
                "El stream de esta radio no esta disponible"
            }
            message.contains("decoder") || message.contains("format") || message.contains("mime") -> {
                "Formato de audio no soportado"
            }
            else -> "No se pudo reproducir la emisora"
        }
    }
}

private fun RadioStation.toMediaItem(index: Int): MediaItem {
    val resolvedUrl = resolvePlayableStreamUrl(url)
    val mediaId = index.toString()
    Log.i("RADIO_CTRL", "toMediaItem(): station=$name rawUrl=$url resolvedUrl=$resolvedUrl mediaId=$mediaId index=$index")
    val guessedMimeType = when {
        resolvedUrl.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
        resolvedUrl.contains(".mp3", ignoreCase = true) -> MimeTypes.AUDIO_MPEG
        resolvedUrl.contains("aac", ignoreCase = true) -> MimeTypes.AUDIO_AAC
        else -> null
    }
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(resolvedUrl)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
                .build(),
        )
        .setMimeType(guessedMimeType)
        .build()
}

private fun resolvePlayableStreamUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return rawUrl

    val parsed = runCatching { URI(trimmed) }.getOrNull() ?: return trimmed
    val host = parsed.host?.lowercase().orEmpty()
    val path = parsed.path.orEmpty()

    // Many Zeno entries include expired signed query params. The canonical stream path works without them.
    if (host.endsWith("zeno.fm") && path.isNotBlank()) {
        return "${parsed.scheme}://$host$path"
    }

    return trimmed
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
