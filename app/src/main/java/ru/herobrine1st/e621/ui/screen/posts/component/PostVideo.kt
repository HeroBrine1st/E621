package ru.herobrine1st.e621.ui.screen.posts.component

import android.util.Log
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.first
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.preference.updatePreferences
import ru.herobrine1st.e621.ui.component.video.VideoPlayer
import ru.herobrine1st.e621.ui.component.video.VideoPlayerState
import ru.herobrine1st.e621.ui.component.video.rememberVideoPlayerState
import ru.herobrine1st.e621.ui.screen.posts.InvalidPost
import ru.herobrine1st.e621.util.debug

private const val TAG = "PostVideo"

@Composable
fun PostVideo(
    file: NormalizedFile,
    aspectRatio: Float = file.aspectRatio,
    videoPlayerState: VideoPlayerState = rememberVideoPlayerState()
) {
    if (aspectRatio <= 0) {
        InvalidPost(stringResource(R.string.invalid_post_server_error))
        return
    }

    VideoPlayer(
        modifier = Modifier.aspectRatio(aspectRatio),
        state = videoPlayerState
    )
    HandlePreferences(videoPlayerState)
}

@Composable
fun HandlePreferences(state: VideoPlayerState) {
    val context = LocalContext.current
    LaunchedEffect(state) {
        val preferences = context.getPreferencesFlow().first()
        state.isMuted = preferences.muteSoundOnMedia
        state.showRemaining = preferences.showRemainingTimeMedia
        debug {
            Log.d(
                TAG,
                "Setting state - Mute: ${state.isMuted}, Show remaining: ${state.showRemaining}"
            )
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.isMuted }.collect {
            context.updatePreferences { muteSoundOnMedia = it }
            debug {
                Log.d(TAG, "Updating preferences: MUTE_SOUND_MEDIA=${it}")
            }
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.showRemaining }.collect {
            context.updatePreferences { showRemainingTimeMedia = it }
            debug {
                Log.d(TAG, "Updating preferences: SHOW_REMAINING_TIME_MEDIA=${state.showRemaining}")
            }
        }
    }
}