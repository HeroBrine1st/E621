package ru.herobrine1st.e621.ui.screen.posts.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.component.OutlinedChip
import ru.herobrine1st.e621.ui.screen.posts.InvalidPost


@Composable
fun PostImage(
    post: Post,
    openPost: (() -> Unit)?,
    file: NormalizedFile,
    aspectRatio: Float = file.aspectRatio
) {
    if (aspectRatio <= 0) {
        InvalidPost(stringResource(R.string.invalid_post_server_error))
        return
    }

    val modifier = if (openPost == null) Modifier else Modifier.clickable {
        openPost()
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

