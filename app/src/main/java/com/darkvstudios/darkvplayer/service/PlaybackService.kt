package com.darkvstudios.darkvplayer.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.darkvstudios.darkvplayer.logic.PlaylistManager

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var playlistManager: PlaylistManager

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        playlistManager = PlaylistManager(this)
        
        // Оптимізація: back buffer вимкнено для економії RAM
        val loadControl = DefaultLoadControl.Builder()
            .setBackBuffer(0, false)
            .build()

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        mediaSession = MediaSession.Builder(this, player).build()
        
        val activePath = playlistManager.getActivePlaylistPath()
        if (activePath != null) {
            val songs = playlistManager.loadSongsFromFolder(activePath)
            if (songs.isNotEmpty()) {
                player.setMediaItems(songs)
                player.shuffleModeEnabled = playlistManager.getShuffleMode()
                player.prepare()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player != null && (!player.playWhenReady || player.mediaItemCount == 0)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
