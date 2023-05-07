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

package ru.herobrine1st.e621.ui.component.video.legacy

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.StyledPlayerView.SHOW_BUFFERING_NEVER
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.navigation.component.CONTROLS_TIMEOUT_MS
import ru.herobrine1st.e621.ui.component.post.HandlePreferences

@Suppress("unused")
private const val TAG = "VideoPlayer"

@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = false,
    repeatMode: Int = ExoPlayer.REPEAT_MODE_ALL,
    state: VideoPlayerState = rememberVideoPlayerState(
        initialPlayWhenReady = playWhenReady,
        initialRepeatMode = repeatMode
    ),
    maxHeight: Dp = Dp.Unspecified,
    aspectRatio: Float? = null,
    controlsTimeoutMs: Long = CONTROLS_TIMEOUT_MS
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<VideoPlayerViewModel>()

    fun resetHideControlsDeadline() {
        state.hideControlsDeadlineMs = System.currentTimeMillis() + controlsTimeoutMs
    }


    Box(modifier = modifier
        .toggleable(
            state.showControls,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onValueChange = { state.showControls = it }
        )
        .heightIn(max = maxHeight),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.background(Color.Black)
                .run {
                    if (aspectRatio != null) aspectRatio(aspectRatio)
                    else this
                },
            factory = {
                StyledPlayerView(context).apply {
                    useController = false
                    setShowBuffering(SHOW_BUFFERING_NEVER)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = {
                it.player = viewModel.exoPlayer
            }
        )
        AnimatedVisibility(
            visible = state.showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize()
        ) {

        }
        AnimatedVisibility(
            visible = viewModel.playbackState == Player.STATE_BUFFERING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }

    /**
     * Hide after [controlsTimeoutMs] from click. Cancel if is hidden by user.
     */
    if (state.controlsAutoHide && state.showControls) DisposableEffect(Unit) {
        resetHideControlsDeadline()
        val job = scope.launch {
            delay(controlsTimeoutMs)
            while (System.currentTimeMillis() < state.hideControlsDeadlineMs) {
                delay(state.hideControlsDeadlineMs - System.currentTimeMillis())
            }
            state.showControls = false
        }
        onDispose {
            job.cancel()
        }
    }

    LaunchedEffect(state.repeatMode) {
        viewModel.exoPlayer.repeatMode = state.repeatMode
    }

    LaunchedEffect(state.playWhenReady) {
        viewModel.playWhenReady = state.playWhenReady
    }

    LaunchedEffect(viewModel.playWhenReady) {
        state.playWhenReady = viewModel.playWhenReady
    }

    LaunchedEffect(state.isMuted) {
        with(viewModel.exoPlayer) {
            if (state.isMuted) {
                volume = 0f
                setAudioAttributes(AudioAttributes.DEFAULT, false)
            } else {
                volume = 1f
                setAudioAttributes(AudioAttributes.DEFAULT, true)
            }
        }
    }
}

@Composable
@Preview
fun PreviewExoPlayer() {
    val state = remember { VideoPlayerState() }
    VideoPlayer(
        state = state
    )
    HandlePreferences(state)
}