package ru.herobrine1st.e621.ui.component.video

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberMediaPlayer(
    uri: Uri,
    playWhenReady: Boolean
): MediaPlayer {
    val context = LocalContext.current
    val mediaPlayer = remember(uri) {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(context, uri)
            setScreenOnWhilePlaying(true)
            if (playWhenReady) {
                setOnPreparedListener {
                    it.start()
                }
            }
        }
    }

    DisposableEffect(mediaPlayer) {
        mediaPlayer.prepareAsync()
        onDispose {
            mediaPlayer.release()
        }
    }
    return mediaPlayer
}