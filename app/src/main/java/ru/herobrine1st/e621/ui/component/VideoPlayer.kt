/*
    This file is full of workarounds and shitty code
*/

package ru.herobrine1st.e621.ui.component

import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.Bundleable
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.StyledPlayerView.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.preference.MUTE_SOUND_MEDIA
import ru.herobrine1st.e621.preference.SHOW_REMAINING_TIME_MEDIA
import ru.herobrine1st.e621.preference.getPreference
import ru.herobrine1st.e621.preference.setPreference
import ru.herobrine1st.e621.util.debug

const val TAG = "VideoPlayer"
const val OVERLAY_TIMEOUT_MS = 7500L

@Composable
fun VideoPlayer(
    uri: String,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = false,
    repeatMode: Int = ExoPlayer.REPEAT_MODE_ALL
) {
    VideoPlayer(MediaItem.fromUri(uri), modifier = modifier, playWhenReady, repeatMode)
}


@Composable
fun VideoPlayer(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = false,
    repeatMode: Int = ExoPlayer.REPEAT_MODE_ALL
) {
    val context = LocalContext.current
    val mute = context.getPreference(MUTE_SOUND_MEDIA, defaultValue = true)
    val exoPlayer: ExoPlayer = rememberExoPlayer {
        setMediaItem(mediaItem)
        this.repeatMode = repeatMode
        this.playWhenReady = playWhenReady
        prepare()
        this.volume = 0f
    }

    LaunchedEffect(mute) {
        with(exoPlayer) {
            if (mute) {
                volume = 0f
                setAudioAttributes(AudioAttributes.DEFAULT, false)
            } else {
                volume = 1f
                setAudioAttributes(AudioAttributes.DEFAULT, true)
            }
        }
    }

    VideoPlayer(exoPlayer = exoPlayer, modifier = modifier)
}

@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(false) }
    val isPlaying by unstableObjectAsState { exoPlayer.isPlaying }
    val isLoading by unstableObjectAsState { exoPlayer.isLoading }

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
                it.player = exoPlayer
            }
        )
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            VideoPlayerController(
                exoPlayer,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f)),
                showRemaining = showRemaining,
                mute = mute // Fix unwanted animation on composition
            )

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
            visible = isLoading && !isPlaying,
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
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
    showRemaining: Boolean,
    mute: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val contentPosition by unstableObjectAsState { exoPlayer.contentPosition }
    val isPlaying by unstableObjectAsState { exoPlayer.isPlaying }
    val isLoading by unstableObjectAsState { exoPlayer.isLoading }


    Box(
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = !isLoading || isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                Alignment.Center
            )
        ) {
            IconButton(
                onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }
            ) {
                Crossfade(targetState = isPlaying) {
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
            val contentPositionSeconds = contentPosition / 1000
            val contentDurationSeconds = exoPlayer.contentDuration / 1000
            Text(
                DateUtils.formatElapsedTime(contentPositionSeconds),
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
            Box(
                Modifier.weight(1f),
                contentAlignment = Alignment.Center // idk why is this line needed, but without it works like Column (???)
            ) {
                LinearProgressIndicator(
                    progress = exoPlayer.bufferedPosition.toFloat() / exoPlayer.contentDuration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    color = Color.Gray
                )
                Slider(
                    value = contentPosition.toFloat(),
                    onValueChange = {
                        exoPlayer.seekTo(it.toLong())
                    },
                    valueRange = 0f..(exoPlayer.contentDuration.toFloat().coerceAtLeast(0.1f)),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.Red,
                        thumbColor = Color.Red,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                DateUtils.formatElapsedTime(if (showRemaining) contentDurationSeconds - contentPositionSeconds else contentDurationSeconds),
                color = Color.White,
                modifier = Modifier
                    .toggleable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, color = Color.White),
                        value = showRemaining,
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
                    context.setPreference(MUTE_SOUND_MEDIA, !mute)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .absoluteOffset((-8).dp, 8.dp)
        ) {
            Crossfade(targetState = mute) { // Show actual state of volume
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
fun rememberExoPlayer(builder: ExoPlayer.() -> Unit = {}): ExoPlayer {
    val context = LocalContext.current
    val exoPlayer = rememberSaveable(saver = Saver( // Save position (at least)
        save = { it.contentPosition },
        restore = {
            ExoPlayer.Builder(context).build().apply(builder).apply { seekTo(it) }
        }
    )) {
        ExoPlayer.Builder(context).build().apply(builder)
    }
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    return exoPlayer
}

@Composable
@Preview
fun PreviewExoPlayer() {
    VideoPlayer(uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
}

@Composable
fun <R> unstableObjectAsState(periodMs: Long = 100L, getter: () -> R): State<R> {
    return produceState(getter()) {
        while (true) {
            delay(periodMs)
            value = getter()
        }
    }
}