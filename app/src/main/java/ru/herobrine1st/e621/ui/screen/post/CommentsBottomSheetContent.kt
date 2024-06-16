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

package ru.herobrine1st.e621.ui.screen.post

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.common.CommentData
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.component.endOfPagePlaceholder
import ru.herobrine1st.e621.ui.screen.post.component.PostComment
import ru.herobrine1st.paging.api.LoadState
import ru.herobrine1st.paging.api.PagingItems

@Composable
fun CommentsBottomSheetContent(
    comments: PagingItems<CommentData>,
    post: Post,
    safeModeEnabled: Boolean
) {
    val commentsLazyListState = rememberLazyListState()

    Box(
        Modifier.fillMaxSize()
    ) {
        if (comments.loadStates.refresh is LoadState.NotLoading) return@Box

        Crossfade(
            comments.loadStates.refresh is LoadState.Error,
            label = "Comments sheet animation between error and content"
        ) {
            if (it) Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Not very good
                // I'm a bad UI designer, I know
                Icon(Icons.Outlined.Error, contentDescription = null)
                Text(stringResource(R.string.comments_load_failed))
                Button(onClick = {
                    comments.refresh()
                }) {
                    Text(stringResource(R.string.retry))
                }
            }
            else LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                state = commentsLazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(
                        WindowInsets.navigationBars.only(
                            WindowInsetsSides.Bottom
                        )
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = with(LocalDensity.current) {
                    PaddingValues(
                        bottom = WindowInsets.navigationBars.getBottom(this).toDp()
                    )
                }
            ) {
                item {}
                endOfPagePlaceholder(comments.loadStates.prepend)
                items(
                    // TODO Move placeholders to pager
                    count = if (comments.loadStates.refresh is LoadState.Loading) post.commentCount
                    else comments.size,
                    key = { index ->
                        val comment = if (index >= comments.size) null
                        else comments[index]
                        return@items comment?.id ?: "index key $index"
                    }
                    // contentType is purposely ignored as all items are of the same type
                ) { index ->
                    val comment =
                        if (index >= comments.size) CommentData.PLACEHOLDER
                        else comments[index]
                    PostComment(
                        comment,
                        safeModeEnabled,
                        modifier = Modifier.padding(horizontal = BASE_PADDING_HORIZONTAL),
                        placeholder = comment === CommentData.PLACEHOLDER,
                        animateTextChange = true
                    )
                }
                endOfPagePlaceholder(comments.loadStates.append)
                item {}
            }
        }
    }

}