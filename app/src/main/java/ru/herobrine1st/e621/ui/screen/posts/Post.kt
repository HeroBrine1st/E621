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

package ru.herobrine1st.e621.ui.screen.posts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.component.post.PostActionRow
import ru.herobrine1st.e621.ui.component.post.PostMediaContainer
import ru.herobrine1st.e621.util.FavouritesCache
import ru.herobrine1st.e621.util.text

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Post(
    post: Post,
    favouriteState: FavouritesCache.FavouriteState,
    isAuthorized: Boolean,
    onFavouriteChange: () -> Unit,
    openPost: (scrollToComments: Boolean) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (post.normalizedSample.type.isVideo) {
                Text(
                    stringResource(
                        R.string.assertion_failed,
                        "API_RETURNED_VIDEO_SAMPLE_${post.id.value}"
                    )
                )
            } else
                PostMediaContainer(
                    file = post.normalizedSample,
                    contentDescription = remember(post.id) { post.tags.all.joinToString(" ") },
                    modifier = Modifier.clickable {
                        openPost(false)
                    },
                    post = post,
                    getVideoPlayerComponent = {
                        throw RuntimeException("Normalized sample is a video, which is not possible")
                    }
                )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                var expandTags by remember { mutableStateOf(false) }
                val visibleTags by remember(post.tags) {
                    derivedStateOf {
                        post.tags.reduced
                            .let {
                                if (expandTags) it
                                else it.take(6)
                            }
                    }
                }
                visibleTags.forEach {
                    InputChip(
                        selected = false,
                        onClick = { /*TODO*/ },
                        label = {
                            Text(it.text)
                        }
                    )
                }
                // TODO use SubcomposeLayout to fill two lines of chips
                if (!expandTags && post.tags.reduced.size > 6) {
                    InputChip(
                        selected = false,
                        onClick = { expandTags = true },
                        label = {
                            Text("...")
                        }
                    )
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 8.dp))
            PostActionRow(
                post, favouriteState, isAuthorized,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                onFavouriteChange = onFavouriteChange
            ) {
                openPost(true)
            }
        }
    }
}