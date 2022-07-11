package ru.herobrine1st.e621.ui.component.video

import android.os.Build
import android.text.format.DateUtils
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.StyledPlayerView.SHOW_BUFFERING_NEVER
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.component.post.HandlePreferences
import kotlin.math.roundToLong

@Suppress("unused")
private const val TAG = "VideoPlayer"
const val CONTROLS_TIMEOUT_MS = 7500L

@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = false,
    repeatMode: Int = ExoPlayer.REPEAT_MODE_ALL,
    state: VideoPlayerState = rememberVideoPlayerState(
        initialPlayWhenReady = playWhenReady,
        initialRepeatMode = repeatMode
    ),
    controlsTimeoutMs: Long = CONTROLS_TIMEOUT_MS
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = hiltViewModel<VideoPlayerViewModel>()

    fun resetHideControlsDeadline() {
        state.hideControlsDeadlineMs = System.currentTimeMillis() + controlsTimeoutMs
    }


    Box(modifier = modifier.toggleable(
        state.showControls,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onValueChange = { state.showControls = it }
    )) {
        AndroidView(
            modifier = Modifier.background(Color.Black),
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
                it.player = viewModel.exoPlayer
            }
        )
        AnimatedVisibility(visible = state.showControls, enter = fadeIn(), exit = fadeOut()) {
            VideoPlayerController(
                contentDurationMs = viewModel.contentDurationMs,
                getContentBufferedPositionMs = { viewModel.exoPlayer.contentBufferedPosition },
                isMuted = state.isMuted,
                isPlaying = viewModel.isPlaying,
                isPlayingWhenReady = viewModel.playWhenReady,
                playbackState = viewModel.playbackState,
                showRemaining = state.showRemaining,
                timestamp = viewModel.timestamp,
                seekTo = { resetHideControlsDeadline(); viewModel.exoPlayer.seekTo(it) },
                toggleMute = { resetHideControlsDeadline(); state.isMuted = it },
                togglePlay = { resetHideControlsDeadline(); viewModel.playWhenReady = it },
                toggleShowRemaining = { resetHideControlsDeadline(); state.showRemaining = it },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f))
            )
        }
        AnimatedVisibility(
            visible = viewModel.playbackState == Player.STATE_BUFFERING,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                Alignment.Center
            )
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }

    /**
     * Hide after [controlsTimeoutMs] from click. Cancel if is hidden by user.
     */
    if (state.controlsAutoHide && state.showControls) DisposableEffect(Unit) {
        resetHideControlsDeadline()
        val job = scope.launch {
            delay(controlsTimeoutMs)
            while (System.currentTimeMillis() < state.hideControlsDeadlineMs) {
                delay(state.hideControlsDeadlineMs - System.currentTimeMillis())
            }
            state.showControls = false
        }
        onDispose {
            job.cancel()
        }
    }

    LaunchedEffect(state.repeatMode) {
        viewModel.exoPlayer.repeatMode = state.repeatMode
    }

    LaunchedEffect(state.playWhenReady) {
        viewModel.playWhenReady = state.playWhenReady
    }

    LaunchedEffect(viewModel.playWhenReady) {
        state.playWhenReady = viewModel.playWhenReady
    }

    LaunchedEffect(state.isMuted) {
        with(viewModel.exoPlayer) {
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
    timestamp: VideoPlayerViewModel.Timestamp,
    isPlaying: Boolean,
    isPlayingWhenReady: Boolean,
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
            visible = playbackState == Player.STATE_READY,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                Alignment.Center
            )
        ) {
            IconButton(
                onClick = { togglePlay(!isPlayingWhenReady) }
            ) {
                Crossfade(targetState = isPlayingWhenReady) {
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
            val context = LocalContext.current
            val contentPositionMs by produceState(
                initialValue = timestamp.contentPositionMs,
                timestamp, isPlaying
            ) {
                value = timestamp.contentPositionMs
                // Use device frame rate if possible, else assume 60 Hz display
                val frameTimeMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        (context.display?.refreshRate?.let { 1000/it })?.roundToLong() ?: 16 else 16
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
                contentAlignment = Alignment.Center // idk why is this line needed, but without it works like Column (???)
            ) {
                LinearProgressIndicator(
                    progress = getContentBufferedPositionMs().toFloat() / contentDurationMs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    color = Color.Gray
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
    val state = remember { VideoPlayerState() }
    VideoPlayer(
        state = state
    )
    HandlePreferences(state)
}