/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import okhttp3.OkHttpClient
import ru.herobrine1st.e621.module.DataStoreModule
import ru.herobrine1st.e621.navigation.component.VideoPlayerComponent
import ru.herobrine1st.e621.util.PreviewUtils
import ru.herobrine1st.e621.util.getPreviewComponentContext

@Composable
@androidx.annotation.OptIn(UnstableApi::class)
fun VideoPlayer(
    component: VideoPlayerComponent,
    modifier: Modifier = Modifier,
    aspectRatio: Float? = null,
    maxHeight: Dp = Dp.Unspecified,
    matchHeightConstraintsFirst: Boolean = false
) {
    val context = LocalContext.current

    Box(modifier = modifier
        .toggleable(
            component.showControls,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onValueChange = { component.showControls = it }
        )
        .heightIn(max = maxHeight),
        contentAlignment = Alignment.Center
    ) {
        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // fixed in Kotlin 2.3 https://github.com/JetBrains/kotlin/commit/b6067c557c7a80ab9426a51e664bc7d1289a27fc
        AndroidView(
            modifier = Modifier
                .background(Color.Black)
                .run {
                    if (aspectRatio != null) aspectRatio(
                        aspectRatio,
                        matchHeightConstraintsFirst
                    ) else this
                },
            factory = {
                PlayerView(context).apply {
                    useController = false
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

        AnimatedVisibility(
            visible = component.showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize()
        ) {
            VideoPlayerController(
                contentDurationMs = component.contentDurationMs,
                getContentBufferedPositionMs = { component.player.contentBufferedPosition },
                isMuted = component.isMuted,
                isPlaying = component.isPlaying,
                willPlayWhenReady = component.playWhenReady,
                playbackState = component.playbackState,
                showRemaining = component.showRemainingInsteadOfTotalTime,
                timestamp = component.timestamp,
                seekTo = { component.resetControlsTimeout(); component.player.seekTo(it) },
                toggleMute = { component.resetControlsTimeout(); component.isMuted = it },
                togglePlay = { component.resetControlsTimeout(); component.playWhenReady = it },
                toggleShowRemaining = {
                    component.resetControlsTimeout(); component.showRemainingInsteadOfTotalTime = it
                },
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.1f))
            )

        }
        AnimatedVisibility(
            visible = component.playbackState == Player.STATE_BUFFERING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@OptIn(PreviewUtils::class)
@Composable
@Preview
private fun Preview() {
    MaterialTheme {
        VideoPlayer(
            component = VideoPlayerComponent(
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                LocalContext.current.applicationContext,
                remember { OkHttpClient() },
                getPreviewComponentContext(),
                DataStoreModule(LocalContext.current)
            )
        )
    }
}