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

package ru.herobrine1st.e621.ui.screen.posts.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post

@Composable
fun PostActionsRow(
    post: Post,
    isFavourite: Boolean,
    isAuthorized: Boolean,
    modifier: Modifier = Modifier,
    onAddToFavourites: () -> Unit,
    onOpenComments: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { /*TODO*/ }, enabled = isAuthorized) {
                Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(R.string.score_up)
                )
            }
            Text(post.score.total.toString())
            IconButton(onClick = { /*TODO*/ }, enabled = isAuthorized) {
                Icon(
                    Icons.Filled.ArrowDownward,
                    contentDescription = stringResource(R.string.score_down)
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 24.dp)
            ) { onOpenComments() }
        ) {
            Text(post.commentCount.toString())
            Icon(
                Icons.Outlined.Comment,
                contentDescription = stringResource(R.string.comments),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .offset(y = 2.dp)
            )
        }
        IconButton(
            onClick = {
                onAddToFavourites()
            },
            enabled = isAuthorized
        ) {
            Crossfade(targetState = isFavourite) {
                if (it) Icon(
                    Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.remove_from_favourites)
                ) else Icon(
                    Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.add_to_favourites)
                )
            }
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                Icons.Default.Share,
                contentDescription = stringResource(R.string.share)
            )
        }
    }
}