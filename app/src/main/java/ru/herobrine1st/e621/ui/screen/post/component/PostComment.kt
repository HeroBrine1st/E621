package ru.herobrine1st.e621.ui.screen.post.component

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import ru.herobrine1st.e621.api.model.CommentBB
import ru.herobrine1st.e621.api.model.PostReduced
import ru.herobrine1st.e621.ui.component.RenderBB

@Composable
fun PostComment(
    comment: CommentBB,
    avatarPost: PostReduced?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CommentAvatar(avatarPost, Modifier.size(24.dp))
            Spacer(Modifier.width(4.dp))
            Text(text = comment.creatorName, fontWeight = FontWeight.Medium, fontSize = 12.sp)

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
            Text(text = comment.score.toString())

        }
        Spacer(Modifier.height(4.dp))
        RenderBB(text = comment.body)
    }
}

@Composable
fun PostCommentPlaceholder(modifier: Modifier = Modifier, lines: Int = 2) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CommentAvatar(null, Modifier.size(24.dp), placeholder = true)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Some text",
                modifier = Modifier.placeholder(true, highlight = PlaceholderHighlight.shimmer())
            )

            Spacer(Modifier.width(2.dp))
            Text(
                text = "1 day ago",
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                modifier = Modifier.placeholder(true, highlight = PlaceholderHighlight.shimmer())
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "100",
                modifier = Modifier.placeholder(true, highlight = PlaceholderHighlight.shimmer())
            )

        }
        Spacer(Modifier.height(4.dp))
        Text(
            (1..lines).joinToString("\n") { it.toString() },
            modifier = Modifier
                .fillMaxWidth() // For placeholder
                .placeholder(true, highlight = PlaceholderHighlight.fade())
        )
    }
}