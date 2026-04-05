package com.app.radiosatelital

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val mediaSessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            Log.i(
                TAG_SERVICE,
                "onConnect(): pkg=${controller.packageName} anunciando comandos completos + previous/next/play/stop",
            )
            val playerCommands = Player.Commands.Builder()
                .addAllCommands()
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_STOP)
                .build()

            val customLayout = listOf(
                CommandButton.Builder()
                    .setDisplayName("Anterior")
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .setIconResId(android.R.drawable.ic_media_previous)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName("Siguiente")
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .setIconResId(android.R.drawable.ic_media_next)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName("Detener")
                    .setPlayerCommand(Player.COMMAND_STOP)
                    .setIconResId(android.R.drawable.ic_menu_close_clear_cancel)
                    .build(),
            )

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS)
                .setAvailablePlayerCommands(playerCommands)
                .setCustomLayout(customLayout)
                .build()
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            Log.i(TAG_SERVICE, "onPostConnect(): controller conectado pkg=${controller.packageName}")
        }

        override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
            Log.i(TAG_SERVICE, "onDisconnected(): controller desconectado pkg=${controller.packageName}")
        }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val resolved = mediaItems.map(::resolveMediaItem)
            return Futures.immediateFuture(resolved)
        }

        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaItemsWithStartPosition> {
            val resolved = mediaItems.map(::resolveMediaItem)
            return Futures.immediateFuture(
                MediaItemsWithStartPosition(resolved, startIndex, startPositionMs),
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG_SERVICE, "onCreate(): iniciando PlaybackService")

        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("ExoPlayer")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(20_000)
            .setDefaultRequestProperties(
                mapOf(
                    "Icy-MetaData" to "1",
                ),
            )

        player = androidx.media3.exoplayer.ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this, extractorsFactory)
                    .setDataSourceFactory(httpDataSourceFactory),
            )
            .build()
            .apply {
                setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                        .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                )
                setHandleAudioBecomingNoisy(true)
                addListener(
                    object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            Log.i(
                                TAG_SERVICE,
                                "player.onPlaybackStateChanged=$playbackState mediaId=${player.currentMediaItem?.mediaId} uri=${player.currentMediaItem?.localConfiguration?.uri}",
                            )
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            Log.i(TAG_SERVICE, "player.onIsPlayingChanged=$isPlaying")
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Log.e(
                                TAG_SERVICE,
                                "player.onPlayerError code=${error.errorCode} name=${error.errorCodeName} msg=${error.message} cause=${error.cause?.message} uri=${player.currentMediaItem?.localConfiguration?.uri}",
                                error,
                            )
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                android.widget.Toast.makeText(
                                    applicationContext,
                                    "Fallo de radio: ${error.errorCodeName}",
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                )
            }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setId(SESSION_ID)
            .setSessionActivity(sessionActivity)
            .setCallback(mediaSessionCallback)
            .build()
        Log.i(TAG_SERVICE, "onCreate(): MediaSession creada")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.i(TAG_SERVICE, "onGetSession(): controller conectado")
        return mediaSession
    }

    override fun onDestroy() {
        Log.i(TAG_SERVICE, "onDestroy(): liberando recursos")
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private companion object {
        const val SESSION_ID = "radio_satelital_playback"
        const val TAG_SERVICE = "RADIO_SERVICE"
    }

    private fun resolveMediaItem(item: MediaItem): MediaItem {
        val originalUri = item.localConfiguration?.uri?.toString()
        if (!originalUri.isNullOrBlank()) {
            Log.i(
                TAG_SERVICE,
                "mediaItem resuelto con URI final: mediaId=${item.mediaId} uri=$originalUri",
            )
            return item
        }

        val index = item.mediaId.toIntOrNull()
        val stationByIndex = index?.let { defaultStations.getOrNull(it) }
        val stationByMediaId = defaultStations.firstOrNull { station ->
            station.url == item.mediaId || normalizeStreamUrl(station.url) == normalizeStreamUrl(item.mediaId)
        }
        val station = stationByIndex ?: stationByMediaId
        return if (station != null) {
            val resolved = MediaItem.Builder()
                .setMediaId(item.mediaId)
                .setUri(normalizeStreamUrl(station.url))
                .setMediaMetadata(
                    item.mediaMetadata
                        .buildUpon()
                        .setTitle(station.name)
                        .setArtist(station.serviceLocationLabel)
                        .build(),
                )
                .build()
            Log.i(
                TAG_SERVICE,
                "mediaItem resuelto con URI final: mediaId=${item.mediaId} uri=${normalizeStreamUrl(station.url)}",
            )
            resolved
        } else {
            Log.e(TAG_SERVICE, "mediaItem sin URI y sin coincidencia válida: mediaId=${item.mediaId}")
            item
        }
    }

    private fun normalizeStreamUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return rawUrl

        val parsed = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return trimmed
        val host = parsed.host?.lowercase().orEmpty()
        val path = parsed.path.orEmpty()

        if (host.endsWith("zeno.fm") && path.isNotBlank()) {
            return "${parsed.scheme}://$host$path"
        }

        return trimmed
    }

    private val RadioStation.serviceLocationLabel: String
        get() = if (region.isBlank()) country else "$country · $region"
}
