/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.herobrine1st.e621.ui.screen.post.component

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.material3.placeholder
import com.google.accompanist.placeholder.material3.shimmer
import ru.herobrine1st.e621.ui.component.RenderBB
import ru.herobrine1st.e621.ui.screen.post.data.CommentData

@Composable
fun PostComment(
    commentData: CommentData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CommentAvatar(commentData.author.avatarUrl, Modifier.size(24.dp))
            Spacer(Modifier.width(4.dp))
            Text(text = commentData.author.displayName, fontWeight = FontWeight.Medium, fontSize = 12.sp)

            Spacer(Modifier.width(2.dp))
            Text(
                text = DateUtils.getRelativeTimeSpanString(
                    commentData.creationTime.toEpochSecond() * 1000,
                    System.currentTimeMillis(),
                    0L,
                    DateUtils.FORMAT_ABBREV_ALL
                ).toString(),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(text = commentData.score.toString())

        }
        Spacer(Modifier.height(4.dp))
        SelectionContainer {
            RenderBB(commentData.message)
        }
    }
}

@Composable
@Preview
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
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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