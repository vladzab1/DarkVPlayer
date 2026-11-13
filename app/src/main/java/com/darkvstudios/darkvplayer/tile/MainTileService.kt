package com.darkvstudios.darkvplayer.tile

import android.content.Context
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import com.darkvstudios.darkvplayer.R
import com.darkvstudios.darkvplayer.logic.PlaylistManager
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.File

class MainTileService : TileService() {

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> {
        val playlistManager = PlaylistManager(this)
        val activePath = playlistManager.getActivePlaylistPath()
        val playlistName = activePath?.let { File(it).name } ?: "DarkV Player"
        val isShuffle = playlistManager.getShuffleMode()

        val layout = if (activePath == null) {
            createEmptyStateLayout()
        } else {
            createMainLayout(playlistName, isShuffle)
        }

        return Futures.immediateFuture(
            Tile.Builder()
                .setResourcesVersion("2")
                .setTimeline(
                    Timeline.Builder()
                        .addTimelineEntry(
                            TimelineEntry.Builder()
                                .setLayout(
                                    LayoutElementBuilders.Layout.Builder()
                                        .setRoot(layout)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        )
    }

    override fun onResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> {
        return Futures.immediateFuture(
            Resources.Builder()
                .setVersion("2")
                .addIdToImageMapping("ic_prev", createIconResource(R.drawable.ic_skip_previous))
                .addIdToImageMapping("ic_play", createIconResource(R.drawable.ic_play_arrow))
                .addIdToImageMapping("ic_next", createIconResource(R.drawable.ic_skip_next))
                .addIdToImageMapping("ic_shuffle", createIconResource(R.drawable.ic_shuffle))
                .build()
        )
    }

    private fun createIconResource(resId: Int): ResourceBuilders.ImageResource {
        return ResourceBuilders.ImageResource.Builder()
            .setAndroidResourceByResId(
                ResourceBuilders.AndroidImageResourceByResId.Builder()
                    .setResourceId(resId)
                    .build()
            ).build()
    }

    private fun createEmptyStateLayout(): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Column.Builder()
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText("No Playlist Selected")
                    .build()
            )
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(8f)).build())
            .addContent(createLaunchButton("Open App"))
            .build()
    }

    private fun createMainLayout(playlistName: String, isShuffle: Boolean): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Column.Builder()
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(playlistName)
                    .build()
            )
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(8f)).build())
            .addContent(
                LayoutElementBuilders.Row.Builder()
                    .addContent(createIconButton("ic_prev"))
                    .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(8f)).build())
                    .addContent(createIconButton("ic_play"))
                    .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(8f)).build())
                    .addContent(createIconButton("ic_next"))
                    .build()
            )
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(8f)).build())
            .addContent(
                LayoutElementBuilders.Row.Builder()
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText(if (isShuffle) "Shuffle: ON" else "Shuffle: OFF")
                            .build()
                    )
                    .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(6f)).build())
                    .addContent(createIconButton("ic_shuffle"))
                    .build()
            )
            .build()
    }

    private fun createIconButton(resId: String): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Image.Builder()
            .setResourceId(resId)
            .setWidth(dp(24f))
            .setHeight(dp(24f))
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setOnClick(
                                ActionBuilders.LaunchAction.Builder()
                                    .setAndroidActivity(
                                        ActionBuilders.AndroidActivity.Builder()
                                            .setPackageName("com.darkvstudios.darkvplayer")
                                            .setClassName("com.darkvstudios.darkvplayer.presentation.MainActivity")
                                            .build()
                                    ).build()
                            ).build()
                    )
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(0xFF333333.toInt()))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(12f)).build())
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun createLaunchButton(text: String): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Box.Builder()
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setOnClick(
                                ActionBuilders.LaunchAction.Builder()
                                    .setAndroidActivity(
                                        ActionBuilders.AndroidActivity.Builder()
                                            .setPackageName("com.darkvstudios.darkvplayer")
                                            .setClassName("com.darkvstudios.darkvplayer.presentation.MainActivity")
                                            .build()
                                    ).build()
                            ).build()
                    )
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(0xFF444444.toInt()))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(8f)).build())
                            .build()
                    )
                    .setPadding(ModifiersBuilders.Padding.Builder().setAll(dp(6f)).build())
                    .build()
            )
            .addContent(LayoutElementBuilders.Text.Builder().setText(text).build())
            .build()
    }
}
