package ru.herobrine1st.e621.ui.screen.post.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import ru.herobrine1st.e621.api.model.PostReduced

@Composable
@OptIn(ExperimentalCoilApi::class)
fun CommentAvatar(avatarPost: PostReduced?, modifier: Modifier = Modifier, placeholder: Boolean = false) {
    val url = avatarPost?.previewUrl ?: avatarPost?.croppedUrl
    if (url != null) {
        val imagePainter = rememberImagePainter(url) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        Image(
            painter = imagePainter,
            modifier = modifier
                .clip(CircleShape) // For placeholder
                .placeholder(
                    imagePainter.state is ImagePainter.State.Loading,
                    highlight = PlaceholderHighlight.fade()
                ),
            contentDescription = null
        )
    } else {
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = null,
            // If placeholder = true, there should be no avatarPost object provided (=null)
            modifier = modifier
                .clip(CircleShape) // For placeholder
                .placeholder(placeholder, highlight = PlaceholderHighlight.fade())
        )
    }
}