package com.darkvstudios.darkvplayer.presentation

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import com.darkvstudios.darkvplayer.R
import com.darkvstudios.darkvplayer.logic.PlaylistManager
import com.darkvstudios.darkvplayer.presentation.theme.DarkVPlayerTheme
import com.darkvstudios.darkvplayer.service.PlaybackService
import com.google.common.util.concurrent.MoreExecutors
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val playlistManager = remember { PlaylistManager(context) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var selectedFolder by remember { mutableStateOf<File?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    DarkVPlayerTheme {
        AppScaffold {
            if (!hasPermission) {
                PermissionScreen { launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
            } else {
                if (selectedFolder == null) {
                    PlaylistSelectionScreen(playlistManager) { folder ->
                        selectedFolder = folder
                    }
                } else {
                    SongSelectionScreen(selectedFolder!!, playlistManager) {
                        selectedFolder = null
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onRequestPermission) {
            Text("Grant Storage Permission")
        }
    }
}

@Composable
fun PlaylistSelectionScreen(playlistManager: PlaylistManager, onFolderSelected: (File) -> Unit) {
    val playlists = remember { playlistManager.getPlaylists() }
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (playlists.isEmpty()) {
                item {
                    Text("No folders in /sdcard/Music/", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                items(playlists.size) { index ->
                    val folder = playlists[index]
                    Button(
                        onClick = { onFolderSelected(folder) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun SongSelectionScreen(folder: File, playlistManager: PlaylistManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val songs = remember(folder) { playlistManager.loadSongsFromFolder(folder.absolutePath) }
    val listState = rememberTransformingLazyColumnState()
    var isShuffle by remember { mutableStateOf(playlistManager.getShuffleMode()) }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skip_previous), // Using it as back
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Text(
                        text = folder.name.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    IconButton(
                        onClick = {
                            val newShuffle = !isShuffle
                            isShuffle = newShuffle
                            playlistManager.setShuffleMode(newShuffle)
                            updateShuffleInService(context, newShuffle)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_shuffle),
                            contentDescription = "Shuffle",
                            modifier = Modifier.size(20.dp),
                            tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }

            items(
                count = songs.size,
                key = { index -> songs[index].mediaId } // Додаємо ключі для стабільності списку
            ) { index ->
                val song = remember(songs[index].mediaId) { songs[index] } // Кешуємо об'єкт пісні
                Button(
                    onClick = {
                        playlistManager.saveActivePlaylist(folder.absolutePath)
                        playSpecificSong(context, songs, index, isShuffle)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = song.mediaMetadata.title?.toString() ?: "Unknown",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun playSpecificSong(context: android.content.Context, songs: List<MediaItem>, startIndex: Int, shuffle: Boolean) {
    val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
    controllerFuture.addListener({
        val controller = controllerFuture.get()
        controller.setMediaItems(songs, startIndex, 0L)
        controller.shuffleModeEnabled = shuffle
        controller.prepare()
        controller.play()
        MediaController.releaseFuture(controllerFuture)
    }, MoreExecutors.directExecutor())
}

private fun updateShuffleInService(context: android.content.Context, shuffle: Boolean) {
    val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
    controllerFuture.addListener({
        val controller = controllerFuture.get()
        controller.shuffleModeEnabled = shuffle
        MediaController.releaseFuture(controllerFuture)
    }, MoreExecutors.directExecutor())
}
