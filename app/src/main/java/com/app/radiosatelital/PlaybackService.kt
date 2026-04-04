package com.app.radiosatelital

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

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
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Log.i(TAG_SERVICE, "player.onIsPlayingChanged=$isPlaying")
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG_SERVICE, "player.onPlayerError", error)
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
