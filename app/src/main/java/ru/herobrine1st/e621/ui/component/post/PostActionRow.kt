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

package ru.herobrine1st.e621.ui.component.post

import android.content.Intent
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.common.VoteResult
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.util.FavouritesCache.FavouriteState
import ru.herobrine1st.e621.util.debug

@Composable
fun PostActionRow(
    post: Post,
    favouriteState: FavouriteState,
    isAuthorized: Boolean,
    modifier: Modifier = Modifier,
    onFavouriteChange: () -> Unit,
    onOpenComments: () -> Unit,
    onVote: suspend (Int) -> VoteResult?,
    getVote: suspend () -> Int,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRemoveFromFavouritesConfirmation by remember { mutableStateOf(false) }

    var vote by rememberSaveable { mutableIntStateOf(-2) }
    var scoreWithoutUs by rememberSaveable { mutableIntStateOf(Int.MIN_VALUE) }

    // Error is ±3, there's just no info from API server
    LaunchedEffect(Unit) {
        if (vote == -2) {
            vote = getVote()
            scoreWithoutUs = post.score.total - vote
        }

        debug {
            snapshotFlow { vote to scoreWithoutUs }.collect {
                Log.d("PostActionRow", "Vote: ${it.first}, scoreWithoutUs: ${it.second}")
            }
        }
    }



    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        onVote((vote + 1).coerceAtMost(1))?.let {
                            vote = it.vote
                            scoreWithoutUs = it.totalScore - it.vote
                        }
                    }
                },
                enabled = isAuthorized && vote != 1
            ) {
                Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(R.string.score_up)
                )
            }
            Text((scoreWithoutUs + vote).toString())
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        onVote((vote - 1).coerceAtLeast(-1))?.let {
                            vote = it.vote
                            scoreWithoutUs = it.totalScore - it.vote
                        }
                    }
                }, enabled = isAuthorized && vote != -1
            ) {
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
                indication = rememberRipple(bounded = false, radius = 24.dp),
                onClick = onOpenComments
            )
        ) {
            Text(post.commentCount.toString())
            Icon(
                Icons.AutoMirrored.Outlined.Comment,
                contentDescription = stringResource(R.string.comments),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .offset(y = 2.dp)
            )
        }
        IconButton(
            onClick = {
                if (!favouriteState.isFavourite)
                    onFavouriteChange()
                else showRemoveFromFavouritesConfirmation = true
            },
            enabled = isAuthorized && favouriteState !is FavouriteState.InFly
        ) {
            Crossfade(
                targetState = favouriteState.isFavourite,
                label = "Favourite icon animation"
            ) {
                if (it) Icon(
                    Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.remove_from_favourites)
                ) else Icon(
                    Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.add_to_favourites)
                )
            }
        }
        IconButton(onClick = {
            context.startActivity(
                Intent.createChooser(
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "${BuildConfig.DEEP_LINK_BASE_URL}/posts/${post.id.value}"
                        )
                        type = "text/plain"
                    }, null
                )
            )
        }) {
            Icon(
                Icons.Default.Share,
                contentDescription = stringResource(R.string.share)
            )
        }
    }

    if (showRemoveFromFavouritesConfirmation) AlertDialog(
        onDismissRequest = {
            showRemoveFromFavouritesConfirmation = false
        },
        title = {
            Text(stringResource(R.string.remove_from_favourites_confirmation_dialog_title))
        },
        text = {
            Text(stringResource(R.string.remove_from_favourites_confirmation_dialog_text))
        },
        confirmButton = {
            Button(onClick = {
                showRemoveFromFavouritesConfirmation = false
                onFavouriteChange()
            }) {
                Text(stringResource(R.string.remove))
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = {
                showRemoveFromFavouritesConfirmation = false
            }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}