package ru.herobrine1st.e621.ui.component.video

import android.media.MediaPlayer
import android.media.MediaTimestamp
import android.media.MediaTimestamp.TIMESTAMP_UNKNOWN
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MediaPlayerState(mediaPlayer: MediaPlayer) {
    var bufferingPercent by mutableStateOf(0)
        private set
    var timestamp: MediaTimestamp by mutableStateOf(TIMESTAMP_UNKNOWN)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var durationMs by mutableStateOf(-1)
        private set


    init {
        mediaPlayer.setOnBufferingUpdateListener { _, percent ->
            bufferingPercent = percent
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mediaPlayer.setOnMediaTimeDiscontinuityListener { mp, mts ->
                timestamp = mts
                isPlaying = mp.isPlaying
            }
        }
        mediaPlayer.setOnPreparedListener {
            durationMs = it.duration
        }
    }
}