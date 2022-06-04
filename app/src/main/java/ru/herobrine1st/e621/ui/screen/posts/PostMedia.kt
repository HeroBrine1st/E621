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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.preference.*
import ru.herobrine1st.e621.ui.component.OutlinedChip
import ru.herobrine1st.e621.ui.component.video.ExoPlayerState
import ru.herobrine1st.e621.ui.component.video.VideoPlayer
import ru.herobrine1st.e621.ui.component.video.rememberExoPlayer

@Composable
fun PostMedia(
    post: Post,
    openPost: ((scrollToComments: Boolean) -> Unit)?,
    file: NormalizedFile
) {
    val aspectRatio = file.width.toFloat() / file.height.toFloat()
    if (aspectRatio <= 0) {
        InvalidPost(stringResource(R.string.invalid_post_server_error))
        return
    }

    if (file.type.isImage) PostImage(post, aspectRatio, openPost, file)
    else if (file.type.isVideo) PostVideo(file, aspectRatio)
    else InvalidPost(stringResource(R.string.unsupported_post_type, file.type.extension))
}

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
fun PostVideo(file: NormalizedFile, aspectRatio: Float) {
    val exoPlayer = rememberExoPlayer(uri = file.urls.first())
    val state = exoPlayer.second

    VideoPlayer(exoPlayer, modifier = Modifier.aspectRatio(aspectRatio))
    HandlePreferences(state)
}

@Composable
fun HandlePreferences(state: ExoPlayerState) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        state.isMuted = context.getPreferenceFlow(MUTE_SOUND_MEDIA, true).first()
        state.showRemaining = context.getPreferenceFlow(SHOW_REMAINING_TIME_MEDIA, true).first()
    }


    // Due to [state.isMuted] here and similar call below compositor should recall this function on
    // every change of either of these values, so I should extract those in another function
    // Or else there will be very strange, hard-to-test (literally - button's click area become so
    // small that you should try to click 10 times) bug which resets state because of go fuck yourself
    LaunchedEffect(state.isMuted) {
        context.setPreference(MUTE_SOUND_MEDIA, state.isMuted)
    }

    LaunchedEffect(state.showRemaining) {
        context.setPreference(SHOW_REMAINING_TIME_MEDIA, state.showRemaining)
    }
}