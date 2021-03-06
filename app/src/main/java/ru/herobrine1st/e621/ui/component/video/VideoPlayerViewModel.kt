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