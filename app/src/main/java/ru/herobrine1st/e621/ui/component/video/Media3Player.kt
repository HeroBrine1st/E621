/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.herobrine1st.e621.ui.component.video

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.Lifecycle
import okhttp3.OkHttpClient
import ru.herobrine1st.e621.util.InstanceBase
import ru.herobrine1st.e621.util.PreviewUtils
import ru.herobrine1st.e621.util.debug
import ru.herobrine1st.e621.util.getPreviewComponentContext

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
fun Media3Player(
    component: Media3PlayerComponent,
    modifier: Modifier = Modifier,
    aspectRatio: Float? = null,
) {
    val context = LocalContext.current


    AndroidView(
        modifier = modifier
            .background(Color.Black)
            .fillMaxSize()
            .run { if (aspectRatio != null) aspectRatio(aspectRatio) else this },
        factory = {
            PlayerView(context).apply {
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = {
            it.player = component.player
        }
    )
}

class Media3PlayerComponent(
    mediaItem: MediaItem,
    applicationContext: Context,
    mediaOkHttpClient: OkHttpClient,
    componentContent: ComponentContext
) : ComponentContext by componentContent, Player.Listener, Lifecycle.Callbacks {
    private val instance = instanceKeeper.getOrCreate {
        Instance(mediaItem, applicationContext, mediaOkHttpClient)
    }

    val player by instance::player

    var timestamp by mutableStateOf(getCurrentTimestamp())
        private set
    var isLoading by mutableStateOf(player.isLoading)
        private set
    var isPlaying by mutableStateOf(player.isPlaying)
        private set
    var contentDurationMs by mutableStateOf(player.contentDuration.coerceAtLeast(0))
        private set

    @get:Player.State
    var playbackState by mutableStateOf(player.playbackState)
        private set

    private var _playWhenReady by mutableStateOf(player.playWhenReady)

    var playWhenReady: Boolean
        get() = _playWhenReady
        set(v) {
            // I know about snapshotFlow, but let's do it without coroutines
            player.playWhenReady = v
            _playWhenReady = v
        }

    init {
        lifecycle.subscribe(this)
        player.addListener(this)
    }

    override fun onResume() {
        debug {
            Log.d(TAG, "Setting playWhenReady = true")
        }
        if (!player.availableCommands.contains(COMMAND_PLAY_PAUSE)) Log.w(
            TAG,
            "playWhenReady modified while command is not available"
        )
        // TODO preference
        // implement as applicationContext.getPreferencesFlow {...}.limit(1).collect { playWhenReady = it }
        playWhenReady = true
    }

    override fun onPause() {
        playWhenReady = false
    }

    override fun onDestroy() {
        player.removeListener(this)
    }

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        if (availableCommands.contains(COMMAND_PLAY_PAUSE)) {
            debug {
                Log.d(
                    TAG,
                    "Player playWhenReady=${player.playWhenReady}, component playWhenReady=$playWhenReady"
                )
            }
            // Just in case
            if (player.playWhenReady != playWhenReady) {
                player.playWhenReady = playWhenReady
            }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        updateTimestamp()
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        this.isLoading = isLoading
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateTimestamp()
        this.isPlaying = isPlaying
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        updateTimestamp()
        contentDurationMs = player.contentDuration.coerceAtLeast(0)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        this.playbackState = playbackState
    }

    private fun updateTimestamp() {
        timestamp = getCurrentTimestamp()
    }

    private fun getCurrentTimestamp() = Timestamp(
        System.currentTimeMillis(),
        player.currentPosition.coerceAtLeast(0),
        player.contentPosition.coerceAtLeast(0),
        player.playbackParameters.speed
    )

    data class Timestamp(
        val anchorMs: Long,
        val positionMs: Long,
        val contentPositionMs: Long,
        val speed: Float
    )

    @androidx.annotation.OptIn(UnstableApi::class)
    private class Instance(
        mediaItem: MediaItem,
        applicationContext: Context,
        mediaOkHttpClient: OkHttpClient
    ) : InstanceBase(), Player.Listener {
        val player = ExoPlayer.Builder(applicationContext)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    OkHttpDataSource.Factory {
                        mediaOkHttpClient.newCall(it)
                    }
                )
            )
            .build()

        init {
            player.setMediaItem(mediaItem)
            player.prepare()
        }

        override fun onDestroy() {
            super.onDestroy()
            player.release()
        }
    }

    companion object {
        private const val TAG = "Media3PlayerController"
    }
}

@OptIn(PreviewUtils::class)
@Composable
@Preview
private fun Preview() {
    Media3Player(
        component = Media3PlayerComponent(
            MediaItem.fromUri("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            LocalContext.current.applicationContext,
            remember { OkHttpClient() },
            getPreviewComponentContext(),
        )
    )
}