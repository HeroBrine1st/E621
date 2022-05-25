/*
    This file is full of workarounds and shitty code
*/

package ru.herobrine1st.e621.ui.component.video

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Build
import android.text.format.DateUtils
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.preference.MUTE_SOUND_MEDIA
import ru.herobrine1st.e621.preference.SHOW_REMAINING_TIME_MEDIA
import ru.herobrine1st.e621.preference.getPreference
import ru.herobrine1st.e621.preference.setPreference

const val TAG = "VideoPlayer"
const val OVERLAY_TIMEOUT_MS = 7500L


@Composable
fun VideoPlayer(
    uri: String,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = false
) {
    val mediaPlayer = rememberMediaPlayer(uri = uri.toUri(), playWhenReady = playWhenReady)

    VideoPlayer(mediaPlayer, modifier)
}


@Composable
fun VideoPlayer(
    mediaPlayer: MediaPlayer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaPlayerState = remember(mediaPlayer) { MediaPlayerState(mediaPlayer) }
    var showControls by remember { mutableStateOf(false) }
    var isPlayDesired by remember { mutableStateOf(false) }

    // TODO fix responsibility
    val showRemaining = context.getPreference(SHOW_REMAINING_TIME_MEDIA, defaultValue = true)
    val mute = context.getPreference(MUTE_SOUND_MEDIA, defaultValue = true)

    Box(modifier = modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = { showControls = !showControls }
    )) {
        AndroidView(
            modifier = Modifier,
            factory = {
                SurfaceView(it)
            },
            update = {
                Log.d(TAG, "IsValid: ${it.holder.surface.isValid}")
                if (it.holder.isCreating || !it.holder.surface.isValid) it.holder.onSurfaceCreated {
                    mediaPlayer.setDisplay(this)
                } else mediaPlayer.setDisplay(it.holder)
            }
        )
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            VideoPlayerController(
                mediaPlayer,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f)),
                shouldShowRemaining = showRemaining,
                isMuted = mute,
                mediaPlayerState,
                isPlayDesired
            ) {
                isPlayDesired = it
                if (it) mediaPlayer.start()
                else mediaPlayer.pause()
            }

            /**
             * Hide after [OVERLAY_TIMEOUT_MS] from click. Cancel if is hidden by user.
             */
            val scope = rememberCoroutineScope()
            DisposableEffect(Unit) {
                val job = scope.launch {
                    delay(OVERLAY_TIMEOUT_MS)
                    showControls = false
                }
                onDispose {
                    job.cancel()
                }
            }
        }
        AnimatedVisibility(
            visible = mediaPlayerState.isPlaying != isPlayDesired // is not interrupted by buffering or something
                    && !mediaPlayerState.isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                Alignment.Center
            )
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }

}


@Composable
fun VideoPlayerController(
    player: MediaPlayer,
    modifier: Modifier = Modifier,
    shouldShowRemaining: Boolean,
    isMuted: Boolean,
    mediaPlayerState: MediaPlayerState,
    isPlayDesired: Boolean,
    onPauseChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()


    Box(
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = mediaPlayerState.isPlaying == isPlayDesired // is buffering
                    || mediaPlayerState.isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                Alignment.Center
            )
        ) {
            IconButton(
                onClick = { onPauseChanged(!isPlayDesired) }
            ) {
                Crossfade(targetState = isPlayDesired) {
                    when (it) {
                        true -> Icon(
                            Icons.Default.Pause,
                            contentDescription = stringResource(R.string.pause),
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
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
            val contentPositionMs by produceState(0, mediaPlayerState.timestamp) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@produceState
                while (true) {
                    @SuppressLint("NewApi")
                    value = ((System.nanoTime() - mediaPlayerState.timestamp.anchorSystemNanoTime) *
                            mediaPlayerState.timestamp.mediaClockRate / 1000).toInt()
                    delay(50)
                }
            }
            val contentPositionSeconds = contentPositionMs / 1000
            val contentDurationSeconds = mediaPlayerState.durationMs / 1000
            Text(
                DateUtils.formatElapsedTime(contentPositionSeconds.toLong()),
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
            Box(
                Modifier.weight(1f),
                contentAlignment = Alignment.Center // idk why is this line needed, but without it works like Column (???)
            ) {
                LinearProgressIndicator(
                    progress = mediaPlayerState.bufferingPercent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    color = Color.Gray
                )
                Slider(
                    value = contentPositionMs.toFloat(),
                    onValueChange = {
                        player.seekTo(it.toInt())
                    },
                    valueRange = 0f..(mediaPlayerState.durationMs.toFloat().coerceAtLeast(0f)),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.Red,
                        thumbColor = Color.Red,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                DateUtils.formatElapsedTime((if (shouldShowRemaining) contentDurationSeconds - contentPositionSeconds else contentDurationSeconds).toLong()),
                color = Color.White,
                modifier = Modifier
                    .toggleable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, color = Color.White),
                        value = shouldShowRemaining,
                        onValueChange = {
                            coroutineScope.launch {
                                context.setPreference(SHOW_REMAINING_TIME_MEDIA, it)
                            }
                        }
                    )
                    .padding(8.dp)
            )
        }

        IconButton(
            onClick = {
                coroutineScope.launch {
                    context.setPreference(MUTE_SOUND_MEDIA, !isMuted)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .absoluteOffset((-8).dp, 8.dp)
        ) {
            Crossfade(targetState = isMuted) { // Show actual state of volume
                if (it) Icon(
                    Icons.Default.VolumeOff,
                    contentDescription = stringResource(R.string.unmute_sound),
                    tint = Color.White
                )
                else Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = stringResource(R.string.mute_sound),
                    tint = Color.White
                )
            }
        }
    }
}


@Composable
@Preview
fun PreviewExoPlayer() {
    VideoPlayer(uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
}