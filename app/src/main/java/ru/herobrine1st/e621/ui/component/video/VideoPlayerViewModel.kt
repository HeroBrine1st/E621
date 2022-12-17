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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    val exoPlayer: ExoPlayer
) : ViewModel(), Player.Listener {
    init {
        exoPlayer.addListener(this)
    }

    override fun onCleared() {
        exoPlayer.removeListener(this)
    }

    var timestamp by mutableStateOf(getCurrentTimestamp())
        private set
    var isLoading by mutableStateOf(exoPlayer.isLoading)
        private set
    var isPlaying by mutableStateOf(exoPlayer.isPlaying)
        private set
    var contentDurationMs by mutableStateOf(exoPlayer.contentDuration.coerceAtLeast(0))
        private set

    @get:Player.State
    var playbackState by mutableStateOf(exoPlayer.playbackState)
        private set

    private var _playWhenReady by mutableStateOf(exoPlayer.playWhenReady)

    var playWhenReady: Boolean
        get() = _playWhenReady
        set(v) {
            exoPlayer.playWhenReady = v
            _playWhenReady = v
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
        contentDurationMs = exoPlayer.contentDuration.coerceAtLeast(0)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        this.playbackState = playbackState
    }

    private fun updateTimestamp() {
        timestamp = getCurrentTimestamp()
    }

    private fun getCurrentTimestamp() = Timestamp(
        System.currentTimeMillis(),
        exoPlayer.currentPosition.coerceAtLeast(0),
        exoPlayer.contentPosition.coerceAtLeast(0),
        exoPlayer.playbackParameters.speed
    )

    data class Timestamp(
        val anchorMs: Long,
        val positionMs: Long,
        val contentPositionMs: Long,
        val speed: Float
    )
}