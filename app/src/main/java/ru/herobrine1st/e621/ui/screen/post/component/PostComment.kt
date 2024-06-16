/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.herobrine1st.e621.api.common.CommentData
import ru.herobrine1st.e621.ui.component.RenderBB
import ru.herobrine1st.e621.ui.component.placeholder.PlaceholderHighlight
import ru.herobrine1st.e621.ui.component.placeholder.material3.fade
import ru.herobrine1st.e621.ui.component.placeholder.material3.placeholder
import ru.herobrine1st.e621.ui.component.placeholder.material3.shimmer

@Composable
fun PostComment(
    commentData: CommentData,
    safeModeEnabled: Boolean,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
    animateTextChange: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CommentAvatar(
                commentData.author.avatarPost,
                safeModeEnabled,
                Modifier.size(24.dp),
                placeholder = placeholder
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = commentData.author.displayName,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                modifier = Modifier.placeholder(
                    placeholder,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = DateUtils.getRelativeTimeSpanString(
                    commentData.creationTime.toEpochMilliseconds(),
                    System.currentTimeMillis(),
                    0L,
                    DateUtils.FORMAT_ABBREV_ALL
                ).toString(),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                modifier = Modifier.placeholder(
                    placeholder,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = commentData.score.toString(),
                Modifier.placeholder(placeholder, highlight = PlaceholderHighlight.shimmer())
            )
        }
        Spacer(Modifier.height(4.dp))
        SelectionContainer(
            modifier = Modifier
                .alpha(if (commentData.isHidden) 0.75f else 1f)
                .fillMaxWidth()
                .placeholder(placeholder, highlight = PlaceholderHighlight.fade())
        ) {
            if (animateTextChange) AnimatedContent(
                targetState = commentData.message,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith
                            fadeOut(animationSpec = tween(90))
                }, label = "Text change animation"
            ) {
                RenderBB(it)
            } else RenderBB(commentData.message)
        }
    }
}