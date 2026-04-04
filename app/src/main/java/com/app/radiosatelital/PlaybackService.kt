package com.app.radiosatelital

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommands

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
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG_SERVICE, "onCreate(): iniciando PlaybackService")

        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
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

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(
                            TAG_SERVICE,
                            "player.onPlayerError code=${error.errorCode} name=${error.errorCodeName} msg=${error.message}",
                            error,
                        )
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
}
