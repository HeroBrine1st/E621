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

import android.os.Build
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.VideoPlayerComponent
import kotlin.math.roundToLong

@Composable
fun VideoPlayerController(
    timestamp: VideoPlayerComponent.Timestamp,
    isPlaying: Boolean,
    willPlayWhenReady: Boolean,
    @Player.State playbackState: Int,
    togglePlay: (Boolean) -> Unit,
    seekTo: (ms: Long) -> Unit,
    getContentBufferedPositionMs: () -> Long, // there's no listener for this so workaround here
    contentDurationMs: Long,
    showRemaining: Boolean,
    toggleShowRemaining: (Boolean) -> Unit,
    isMuted: Boolean,
    toggleMute: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = playbackState == Player.STATE_BUFFERING,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                Alignment.Center
            )
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        AnimatedVisibility(
            visible = playbackState == Player.STATE_READY,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                Alignment.Center
            )
        ) {
            // After migration to Media3 IconButton got indication
            // while there was no indication before (it was a bug, because code after fix below was literally the same)
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(
                        onClick = { togglePlay(!willPlayWhenReady) },
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = willPlayWhenReady, label = "Play-pause crossfade") {
                    when (it) {
                        true -> Icon(
                            Icons.Default.Pause,
                            contentDescription = stringResource(R.string.pause),
                            tint = Color.White,
                            // Looks like those buttons are bigger than they were
                            modifier = Modifier.size(48.dp)
                        )
                        // Particularly this icon is completely different, like it is not only bigger
                        // but spacing between two rectangles is unproportionally bigger too
                        false -> Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.resume),
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0f),
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(horizontal = 4.dp)
        ) {
            val context = LocalContext.current
            val contentPositionMs by produceState(
                initialValue = timestamp.contentPositionMs,
                timestamp, isPlaying
            ) {
                value = timestamp.contentPositionMs
                // Use device frame rate if possible, else assume 60 Hz display
                val frameTimeMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    (context.display?.refreshRate?.let { 1000 / it })?.roundToLong() ?: 16 else 16
                while (isPlaying) {
                    delay(frameTimeMs)
                    with(timestamp) {
                        value =
                            contentPositionMs + ((System.currentTimeMillis() - anchorMs) * speed).toLong()
                    }
                }
            }
            val contentPositionSeconds = contentPositionMs / 1000
            val contentDurationSeconds = contentDurationMs / 1000
            Text(
                DateUtils.formatElapsedTime(contentPositionSeconds),
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
            Box(
                Modifier.weight(1f),
                contentAlignment = Alignment.Center // idk why this line is needed, but without it works like Column (???)
            ) {
                LinearProgressIndicator(
                    progress = { if (contentDurationMs != 0L) getContentBufferedPositionMs().toFloat() / contentDurationMs else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    color = Color.Gray,
                )
                Slider(
                    value = contentPositionMs.toFloat(),
                    onValueChange = {
                        seekTo(it.toLong())
                    },
                    valueRange = 0f..(contentDurationMs.toFloat()),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.Red,
                        thumbColor = Color.Red,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                (if (showRemaining) "-" else "") +
                        DateUtils.formatElapsedTime(if (showRemaining) contentDurationSeconds - contentPositionSeconds else contentDurationSeconds),
                color = Color.White,
                modifier = Modifier
                    .toggleable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, color = Color.White),
                        value = showRemaining,
                        onValueChange = {
                            toggleShowRemaining(it)
                        }
                    )
                    .padding(8.dp)
            )
        }

        IconButton(
            onClick = {
                toggleMute(!isMuted)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .absoluteOffset((-8).dp, 8.dp)
        ) {
            Crossfade(targetState = isMuted, label = "Mute crossfade") { // Show actual state of volume
                if (it) Icon(
                    Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = stringResource(R.string.unmute_sound),
                    tint = Color.White
                )
                else Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.mute_sound),
                    tint = Color.White
                )
            }
        }
    }
}