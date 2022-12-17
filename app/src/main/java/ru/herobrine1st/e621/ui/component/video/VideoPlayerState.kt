/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import ru.herobrine1st.e621.util.debug

private const val TAG = "VideoPlayerState"

class VideoPlayerState(
    initialIsMuted: Boolean = false,
    initialShowRemaining: Boolean = false,
    initialHideControlsDeadlineMs: Long = 0L,
    initialControlsAutoHide: Boolean = true,
    initialShowControls: Boolean = false,
    initialPlayWhenReady: Boolean = false,
    initialRepeatMode: Int = ExoPlayer.REPEAT_MODE_ALL
) {
    var isMuted by mutableStateOf(initialIsMuted)
    var showRemaining by mutableStateOf(initialShowRemaining)
    var hideControlsDeadlineMs by mutableStateOf(initialHideControlsDeadlineMs)
    var controlsAutoHide by mutableStateOf(initialControlsAutoHide)
    var showControls by mutableStateOf(initialShowControls)
    var playWhenReady: Boolean by mutableStateOf(initialPlayWhenReady)

    @get:Player.RepeatMode
    var repeatMode: Int by mutableStateOf(initialRepeatMode)

    companion object {
        val SAVER = Saver<VideoPlayerState, Bundle>(
            save = {
                val bundle = Bundle()
                with(it) {
                    bundle.putBoolean("isMuted", isMuted)
                    bundle.putBoolean("showRemaining", showRemaining)
                    bundle.putLong("hideControlsDeadlineMs", hideControlsDeadlineMs)
                    bundle.putBoolean("controlsAutoHide", controlsAutoHide)
                    bundle.putBoolean("showControls", showControls)
                    bundle.putBoolean("playWhenReady", playWhenReady)
                    bundle.putInt("repeatMode", repeatMode)
                }
                debug {
                    Log.d(TAG, "Saving to $bundle")
                }
                return@Saver bundle
            },
            restore = { bundle ->
                debug {
                    Log.d(TAG, "Restoring from $bundle")
                }
                VideoPlayerState(
                    bundle.getBoolean("isMuted"),
                    bundle.getBoolean("showRemaining"),
                    bundle.getLong("hideControlsDeadlineMs"),
                    bundle.getBoolean("controlsAutoHide"),
                    bundle.getBoolean("showControls"),
                    bundle.getBoolean("playWhenReady"),
                    bundle.getInt("repeatMode"),
                )
            }
        )
    }
}

@Composable
fun rememberVideoPlayerState(
    initialIsMuted: Boolean = false,
    initialShowRemaining: Boolean = false,
    initialControlsAutoHide: Boolean = true,
    initialShowControls: Boolean = false,
    initialPlayWhenReady: Boolean = false,
    initialRepeatMode: Int = ExoPlayer.REPEAT_MODE_ALL
) = rememberSaveable(saver = VideoPlayerState.SAVER) {
    VideoPlayerState(
        initialIsMuted = initialIsMuted,
        initialShowRemaining = initialShowRemaining,
        initialControlsAutoHide = initialControlsAutoHide,
        initialShowControls = initialShowControls,
        initialPlayWhenReady = initialPlayWhenReady,
        initialRepeatMode = initialRepeatMode
    )
}