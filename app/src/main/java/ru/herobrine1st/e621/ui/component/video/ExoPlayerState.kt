package ru.herobrine1st.e621.ui.component.video

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline

class ExoPlayerState(
    private val exoPlayer: ExoPlayer
) : Player.Listener {
    init {
        exoPlayer.addListener(this)
    }

    fun dispose() {
        exoPlayer.removeListener(this)
    }

    // State of exoplayer
    var timestamp by mutableStateOf(Timestamp.UNKNOWN)
        private set
    var isLoading by mutableStateOf(exoPlayer.isLoading)
        private set
    var isPlaying by mutableStateOf(exoPlayer.isPlaying)
        private set
    var contentDurationMs by mutableStateOf(exoPlayer.contentDuration)
        private set

    @get:Player.State
    var playbackState by mutableStateOf(exoPlayer.playbackState)
        private set

    // Our state
    var isMuted by mutableStateOf(false)

    var hideControlsDeadlineMs: Long by mutableStateOf(0)
        private set

    private var _showControls by mutableStateOf(false)
    var showControls: Boolean
        get() = _showControls
        set(v) {
            _showControls = v
            if (v)
                hideControlsDeadlineMs = System.currentTimeMillis() + OVERLAY_TIMEOUT_MS
        }
    var showRemaining by mutableStateOf(false)

    fun mute(muted: Boolean = true) {
        isMuted = muted
    }

    fun unmute() = mute(false)

    /**
     * @param userInteraction true if user defined, false if application defined
     */
    fun play(playing: Boolean = true, userInteraction: Boolean = false) {
        if (playing) exoPlayer.play()
        else exoPlayer.pause()
        if (userInteraction) hideControlsDeadlineMs =
            System.currentTimeMillis() + OVERLAY_TIMEOUT_MS
    }

    fun pause(userInteraction: Boolean = false) = play(false, userInteraction)


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
        contentDurationMs = exoPlayer.contentDuration
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        this.playbackState = playbackState
    }

    private fun updateTimestamp(positionMs: Long? = null) {
        timestamp = Timestamp(
            System.currentTimeMillis(),
            positionMs ?: exoPlayer.currentPosition,
            positionMs ?: exoPlayer.contentPosition,
            exoPlayer.playbackParameters.speed
        )
    }

    data class Timestamp(
        val anchorMs: Long,
        val positionMs: Long,
        val contentPositionMs: Long,
        val speed: Float
    ) {
        companion object {
            val UNKNOWN = Timestamp(-1, -1, -1, 1f)
        }
    }
}