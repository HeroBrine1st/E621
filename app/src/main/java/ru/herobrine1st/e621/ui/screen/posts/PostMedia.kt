package ru.herobrine1st.e621.ui.screen.posts

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.google.android.exoplayer2.ExoPlayer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.preference.MUTE_SOUND_MEDIA
import ru.herobrine1st.e621.preference.SHOW_REMAINING_TIME_MEDIA
import ru.herobrine1st.e621.preference.getPreferenceFlow
import ru.herobrine1st.e621.preference.setPreference
import ru.herobrine1st.e621.ui.component.OutlinedChip
import ru.herobrine1st.e621.ui.component.video.ExoPlayerState
import ru.herobrine1st.e621.ui.component.video.VideoPlayer
import ru.herobrine1st.e621.util.debug

private const val TAG = "PostMedia"

@Composable
fun InvalidPost(text: String) {
    Box(contentAlignment = Alignment.TopCenter) {
        Text(text)
    }
}

@Composable
fun PostImage(
    post: Post,
    aspectRatio: Float,
    openPost: ((scrollToComments: Boolean) -> Unit)?,
    file: NormalizedFile
) {
    if (aspectRatio <= 0) {
        InvalidPost(stringResource(R.string.invalid_post_server_error))
        return
    }

    val modifier = if (openPost == null) Modifier else Modifier.clickable {
        openPost(false)
    }
    Box(contentAlignment = Alignment.TopStart) {
        Image(
            painter = rememberImagePainter(
                file.urls.first(),
                builder = {
                    crossfade(true)
                }
            ),
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
            contentDescription = remember(post.id) { post.tags.all.joinToString(" ") }
        )
        if (post.file.type.isNotImage) OutlinedChip( // TODO
            modifier = Modifier.offset(x = 10.dp, y = 10.dp),
            backgroundColor = Color.Transparent
        ) {
            Text(post.file.type.extension)
        }
    }
}

@Composable
fun PostVideo(
    aspectRatio: Float,
    exoPlayer: Pair<ExoPlayer, ExoPlayerState>
) {
    if (aspectRatio <= 0) {
        InvalidPost(stringResource(R.string.invalid_post_server_error))
        return
    }

    val state = exoPlayer.second

    VideoPlayer(exoPlayer, modifier = Modifier.aspectRatio(aspectRatio))
    HandlePreferences(state)
}

@Composable
fun HandlePreferences(state: ExoPlayerState) {
    val context = LocalContext.current
    LaunchedEffect(state) {
        state.isMuted = context.getPreferenceFlow(MUTE_SOUND_MEDIA, true).first()
        state.showRemaining = context.getPreferenceFlow(SHOW_REMAINING_TIME_MEDIA, true).first()
        debug {
            Log.d(TAG, "Setting state - Mute: ${state.isMuted}, Show remaining: ${state.showRemaining}")
        }
    }

    // Due to [state.isMuted] here and similar call below compositor should recall this function on
    // every change of either of these values, so I should extract those in another function
    // Or else there will be very strange, hard-to-test (literally - button's click area become so
    // small that you should try to click 10 times) bug which resets state because of go fuck yourself
    LaunchedEffect(state) {
        snapshotFlow { state.isMuted }.collect {
            context.setPreference(MUTE_SOUND_MEDIA, it)
            debug {
                Log.d(TAG, "Updating preferences: MUTE_SOUND_MEDIA=${it}")
            }
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.showRemaining }.collect {
            context.setPreference(SHOW_REMAINING_TIME_MEDIA, it)
            debug {
                Log.d(TAG, "Updating preferences: SHOW_REMAINING_TIME_MEDIA=${state.showRemaining}")
            }
        }
    }
}