package com.darkvstudios.darkvplayer.logic

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.File

class PlaylistManager(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences("DarkVPlayerPrefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_ACTIVE_PLAYLIST_PATH = "active_playlist_path"
        const val KEY_SHUFFLE_MODE = "shuffle_mode"
    }

    fun getPlaylists(): List<File> {
        val musicDir = File(Environment.getExternalStorageDirectory(), "Music")
        if (!musicDir.exists() || !musicDir.isDirectory) {
            return emptyList()
        }
        return musicDir.listFiles { file -> file.isDirectory }?.toList() ?: emptyList()
    }

    fun saveActivePlaylist(path: String) {
        sharedPreferences.edit().putString(KEY_ACTIVE_PLAYLIST_PATH, path).apply()
    }

    fun getActivePlaylistPath(): String? {
        return sharedPreferences.getString(KEY_ACTIVE_PLAYLIST_PATH, null)
    }

    fun loadSongsFromFolder(folderPath: String): List<MediaItem> {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) return emptyList()

        return folder.listFiles { file ->
            file.isFile && (file.extension.equals("mp3", ignoreCase = true))
        }?.sortedBy { it.lastModified() }?.map { file ->
            MediaItem.Builder()
                .setMediaId(file.absolutePath)
                .setUri(Uri.fromFile(file))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(file.nameWithoutExtension)
                        .build()
                )
                .build()
        } ?: emptyList()
    }

    fun setShuffleMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SHUFFLE_MODE, enabled).apply()
    }

    fun getShuffleMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_SHUFFLE_MODE, false)
    }
}
