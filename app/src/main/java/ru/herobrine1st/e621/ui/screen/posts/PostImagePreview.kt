package ru.herobrine1st.e621.ui.screen.posts

import androidx.compose.foundation.Image
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
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.component.OutlinedChip

@Composable
fun PostImagePreview(post: Post) {
    val aspectRatio = post.sample.width.toFloat() / post.sample.height.toFloat()
    if (aspectRatio <= 0) {
        Box(contentAlignment = Alignment.TopCenter) {
            Text("Invalid post") // TODO i18n
        }
        return
    }
    Box(contentAlignment = Alignment.TopStart) {
        Image(
            painter = rememberImagePainter(
                post.sample.url,
                builder = {
                    crossfade(true)
                }
            ),
            modifier = Modifier
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