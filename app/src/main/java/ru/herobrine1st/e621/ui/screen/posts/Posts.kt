/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import com.google.accompanist.flowlayout.FlowRow
import dagger.hilt.android.EntryPointAccessors
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.api.SearchOptions
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.endOfPagePlaceholder
import ru.herobrine1st.e621.ui.component.post.PostImage
import ru.herobrine1st.e621.ui.screen.Screen
import ru.herobrine1st.e621.ui.screen.posts.component.PostActionsRow
import ru.herobrine1st.e621.ui.screen.posts.logic.PostsViewModel
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor
import ru.herobrine1st.e621.util.getParcelableCompat

@Composable
fun PostsAppBarActions(navController: NavHostController) {
    IconButton(onClick = {
        val arguments = navController.currentBackStackEntry!!.arguments!!
        navController.navigate(
            Screen.Search.buildRoute {
                addArgument("query", arguments.getParcelableCompat<PostsSearchOptions>("query")!!)
            }
        )
    }) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(R.string.search),
            tint = ActionBarIconColor
        )
    }
}

@Composable
fun Posts(
    searchOptions: SearchOptions,
    isAuthorized: Boolean,
    openPost: (post: Post, scrollToComments: Boolean) -> Unit,
    viewModel: PostsViewModel = viewModel(
        factory = PostsViewModel.provideFactory(
            EntryPointAccessors.fromActivity<PostsViewModel.FactoryProvider>(
                LocalContext.current as Activity
            ).provideFactory(), searchOptions
        )
    )
) {
    val posts = viewModel.postsFlow.collectAsLazyPagingItems()
    val favouritesCache by viewModel.collectFavouritesCacheAsState()
    val lazyListState = rememberLazyListState()

    // Do not reset lazyListState
    if (posts.loadState.refresh !is LoadState.NotLoading) {
        Base {
            Spacer(modifier = Modifier.height(4.dp))
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        state = lazyListState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        endOfPagePlaceholder(posts.loadState.prepend)
        itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
            if (post == null) return@itemsIndexed
            Post(
                post = post,
                // Remove unwanted visual glitch on first post (white corners stick out a mile)
                shape = if (index == 0)
                    MaterialTheme.shapes.medium.copy(
                        topStart = CornerSize(0.dp),
                        topEnd = CornerSize(0.dp)
                    )
                else MaterialTheme.shapes.medium,
                isFavourite = favouritesCache.getOrDefault(post.id, post.isFavorited),
                isAuthorized = isAuthorized,
                onAddToFavourites = {
                    viewModel.handleFavouriteButtonClick(post)
                }
            ) { scrollToComments -> openPost(post, scrollToComments) }
        }
        endOfPagePlaceholder(posts.loadState.append)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Post(
    post: Post,
    shape: CornerBasedShape = MaterialTheme.shapes.medium,
    isFavourite: Boolean,
    isAuthorized: Boolean,
    onAddToFavourites: () -> Unit,
    openPost: (scrollToComments: Boolean) -> Unit
) {
    Card(
        elevation = 4.dp,
        shape = shape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            when {
                post.file.type.isSupported -> PostImage(
                    post = post,
                    openPost = { openPost(false) },
                    file = post.normalizedSample
                )
                else -> InvalidPost(
                    text = stringResource(
                        R.string.unsupported_post_type,
                        post.file.type.extension
                    )
                )
            }
            FlowRow(
                mainAxisSpacing = 4.dp,
                crossAxisSpacing = 2.dp,
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
                    Chip(onClick = { /*TODO*/ }) {
                        Text(it)
                    }
                }
                if (!expandTags && post.tags.reduced.size > 6) {
                    Chip(onClick = { expandTags = true }) {
                        Text("...")
                    }
                }
            }
            Divider(Modifier.padding(horizontal = 8.dp))
            PostActionsRow(
                post, isFavourite, isAuthorized,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                onAddToFavourites
            ) {
                openPost(true)
            }
        }
    }
}

