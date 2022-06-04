package ru.herobrine1st.e621.ui.component.video

import android.text.format.DateUtils
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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.StyledPlayerView.SHOW_BUFFERING_NEVER
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.screen.posts.HandlePreferences

private const val TAG = "VideoPlayer"
const val OVERLAY_TIMEOUT_MS = 7500L

@Composable
fun VideoPlayer(exoPlayer: Pair<ExoPlayer, ExoPlayerState>, modifier: Modifier = Modifier) {
    VideoPlayer(exoPlayer = exoPlayer.first, state = exoPlayer.second, modifier = modifier)
}

@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    state: ExoPlayerState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = { state.showControls = !state.showControls }
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
        AnimatedVisibility(visible = state.showControls, enter = fadeIn(), exit = fadeOut()) {
            VideoPlayerController(
                exoPlayer,
                state,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f))
            )

            /**
             * Hide after [OVERLAY_TIMEOUT_MS] from click. Cancel if is hidden by user.
             */
            val scope = rememberCoroutineScope()
            DisposableEffect(Unit) {
                val job = scope.launch {
                    delay(OVERLAY_TIMEOUT_MS)
                    while (System.currentTimeMillis() < state.hideControlsDeadlineMs) {
                        delay(state.hideControlsDeadlineMs - System.currentTimeMillis())
                    }
                    state.showControls = false
                }
                onDispose {
                    job.cancel()
                }
            }
        }
        AnimatedVisibility(
            visible = state.playbackState == Player.STATE_BUFFERING,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                Alignment.Center
            )
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }

    LaunchedEffect(state.isMuted) {
        with(exoPlayer) {
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
fun VideoPlayerController(
    exoPlayer: ExoPlayer,
    state: ExoPlayerState,
    modifier: Modifier = Modifier
) {
    val timestamp = state.timestamp
    val isPlaying = state.isPlaying

    Box(
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = state.playbackState != Player.STATE_BUFFERING,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                Alignment.Center
            )
        ) {
            IconButton(
                onClick = { state.play(!isPlaying, true) }
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
            val contentPositionMs by produceState(
                initialValue = timestamp.contentPositionMs,
                timestamp, isPlaying
            ) {
                value = timestamp.contentPositionMs
                while (isPlaying) {
                    delay(100)
                    with(timestamp) {
                        value =
                            contentPositionMs + ((System.currentTimeMillis() - anchorMs) * speed).toLong()
                    }
                }
            }
            val contentPositionSeconds = contentPositionMs / 1000
            val contentDurationSeconds = state.contentDurationMs / 1000
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
                    value = contentPositionMs.toFloat(),
                    onValueChange = {
                        exoPlayer.seekTo(it.toLong())
                    },
                    valueRange = 0f..(state.contentDurationMs.toFloat()),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.Red,
                        thumbColor = Color.Red,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                DateUtils.formatElapsedTime(if (state.showRemaining) contentDurationSeconds - contentPositionSeconds else contentDurationSeconds),
                color = Color.White,
                modifier = Modifier
                    .toggleable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, color = Color.White),
                        value = state.showRemaining,
                        onValueChange = {
                            state.showRemaining = it
                        }
                    )
                    .padding(8.dp)
            )
        }

        IconButton(
            onClick = {
                state.isMuted = !state.isMuted
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .absoluteOffset((-8).dp, 8.dp)
        ) {
            Crossfade(targetState = state.isMuted) { // Show actual state of volume
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
fun rememberExoPlayer(
    uri: String,
    playWhenReady: Boolean = false,
    repeatMode: Int = ExoPlayer.REPEAT_MODE_ALL
): Pair<ExoPlayer, ExoPlayerState> =
    rememberExoPlayer(MediaItem.fromUri(uri), playWhenReady, repeatMode)

@Composable
fun rememberExoPlayer(
    mediaItem: MediaItem,
    playWhenReady: Boolean = false,
    repeatMode: Int = ExoPlayer.REPEAT_MODE_ALL
): Pair<ExoPlayer, ExoPlayerState> = rememberExoPlayer {
    setMediaItem(mediaItem)
    this.repeatMode = repeatMode
    this.playWhenReady = playWhenReady
    prepare()
    this.volume = 0f
}

@Composable
fun rememberExoPlayer(
    builder: ExoPlayer.() -> Unit = {}
): Pair<ExoPlayer, ExoPlayerState> {
    val context = LocalContext.current
    val exoPlayer = rememberSaveable(saver = Saver( // Save position (at least)
        save = { it.contentPosition },
        restore = {
            ExoPlayer.Builder(context).build().apply(builder).apply { seekTo(it) }
        }
    )) {
        ExoPlayer.Builder(context).build().apply(builder)
    }
    val state = remember(exoPlayer) { ExoPlayerState(exoPlayer) }
    DisposableEffect(exoPlayer) {
        onDispose {
            state.dispose()
            exoPlayer.release()
        }
    }
    return exoPlayer to state
}

@Composable
@Preview
fun PreviewExoPlayer() {
    val exoPlayer =
        rememberExoPlayer("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
    val state = exoPlayer.second
    VideoPlayer(exoPlayer)
    HandlePreferences(state)
}