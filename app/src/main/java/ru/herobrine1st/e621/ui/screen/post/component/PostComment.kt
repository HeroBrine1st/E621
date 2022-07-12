package ru.herobrine1st.e621.ui.screen.post.component

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.MessageQuote
import ru.herobrine1st.e621.api.MessageText
import ru.herobrine1st.e621.api.model.CommentBB
import ru.herobrine1st.e621.api.model.PostReduced
import ru.herobrine1st.e621.api.parseBBCode
import ru.herobrine1st.e621.ui.theme.disabledText

@Composable
fun PostComment(
    comment: CommentBB,
    avatarPost: PostReduced?,
    modifier: Modifier = Modifier,
    showAsPreview: Boolean = false,
    placeholder: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CommentAvatar(avatarPost, Modifier.size(24.dp), placeholder = placeholder)
            Spacer(Modifier.width(4.dp))
            Text(
                text = comment.creatorName,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                modifier = Modifier.placeholder(
                    placeholder,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )
            if (!placeholder) {
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
                    modifier = Modifier.placeholder(
                        placeholder,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = comment.score.toString(),
                    modifier = Modifier.placeholder(
                        placeholder,
                        highlight = PlaceholderHighlight.shimmer()
                    )
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        val parsed = remember(comment) { parseBBCode(comment.body) }

        if (showAsPreview) {
            val text = remember(parsed) {
                (parsed.firstOrNull { it is MessageText } as MessageText?)?.text
            }
            Text(
                text ?: AnnotatedString(stringResource(parsed.first().getDescription())),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .placeholder(
                        placeholder,
                        highlight = PlaceholderHighlight.shimmer()
                    )
            )
        } else parsed.forEach {
            if (it is MessageQuote) {
                Text(stringResource(R.string.quote_comments, it.userName))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colors.disabledText)
                    )
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            it.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .placeholder(
                                    placeholder,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                }
            } else if (it is MessageText) {
                Text(
                    it.text, modifier = Modifier
                        .fillMaxWidth() // For placeholder
                        .placeholder(placeholder, highlight = PlaceholderHighlight.shimmer())
                )
            }

        }
    }
}