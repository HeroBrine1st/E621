package ru.herobrine1st.e621.ui.screen.posts

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.component.OutlinedChip
import kotlin.math.roundToInt

@Composable
fun PostImagePreview(post: Post, openPost: ((scrollToComments: Boolean) -> Unit)?) {
    val aspectRatio = post.file.width.toFloat() / post.file.height.toFloat()

    if (aspectRatio <= 0) {
        Box(contentAlignment = Alignment.TopCenter) {
            Text("Invalid post") // TODO i18n
        }
        return
    }

    val width = LocalConfiguration.current.let {
        (it.screenWidthDp.toFloat() * it.densityDpi / 160f).roundToInt()
    }

    val modifier = if (openPost == null) Modifier else Modifier.clickable {
        openPost(false)
    }
    Box(contentAlignment = Alignment.TopStart) {
        val file = remember(post.id, width) {
            post.files
                .filter { it.urls.isNotEmpty() } // Это апи мне уже вернуло время в зоне -4, какую хуйню ещё оно мне вернёт - стоит только догадываться
                .let {
                    it.find { it1 -> it1.width >= width } ?: it.last()
                }
        }

        LaunchedEffect(post.id) {
            Log.d("Posts Screen/Post", "Selected file ${file.name} for post ${post.id} (display width: $width, file width: ${file.width}, difference: ${file.width - width}")
        }
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