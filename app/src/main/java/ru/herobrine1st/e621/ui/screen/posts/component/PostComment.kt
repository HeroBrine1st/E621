package ru.herobrine1st.e621.ui.screen.posts.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import ru.herobrine1st.e621.api.model.Comment

@Composable
@OptIn(ExperimentalCoilApi::class)
fun PostComment(comment: Comment) {
    // TODO
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
    ) {
        Row {

            if (comment.avatarPost != null) {
                val imagePainter = rememberImagePainter(comment.avatarPost.previewUrl) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
                Image(
                    painter = imagePainter,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape) // For placeholder
                        .placeholder(
                            imagePainter.state is ImagePainter.State.Loading,
                            highlight = PlaceholderHighlight.fade()
                        ),
                    contentDescription = "avatar" // TODO i18n
                )
            } else {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = "avatar", // TODO i18n
                    modifier = Modifier.size(48.dp)
                )
            }
            Column {
                Text(
                    text = comment.authorName,
                    lineHeight = with(LocalDensity.current) { 24.dp.toSp() }
                )
            }
        }
        Text(comment.content)
    }
}