package ru.herobrine1st.e621.ui.screen.posts.component

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import ru.herobrine1st.e621.api.model.CommentBB
import ru.herobrine1st.e621.api.model.PostReduced

@Composable
@OptIn(ExperimentalCoilApi::class)
fun PostComment(comment: CommentBB, avatarPost: PostReduced?) {
    // TODO
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val url = avatarPost?.previewUrl ?: avatarPost?.croppedUrl
            if (url != null) {
                val imagePainter = rememberImagePainter(url) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
                Image(
                    painter = imagePainter,
                    modifier = Modifier
                        .size(24.dp)
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
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = comment.creatorName,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = DateUtils.getRelativeTimeSpanString(
                    comment.createdAt.toEpochSecond() * 1000,
                    System.currentTimeMillis(),
                    0L,
                    DateUtils.FORMAT_ABBREV_ALL
                ).toString(),
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = comment.score.toString(),
//                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(comment.body) // TODO parse BBcode
    }
}