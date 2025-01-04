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

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.module.CachedDataStore
import ru.herobrine1st.e621.navigation.component.posts.PostListingComponent
import ru.herobrine1st.e621.navigation.component.posts.PostListingComponent.InfoState
import ru.herobrine1st.e621.navigation.component.posts.UIPostListingItem
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.component.endOfPagePlaceholder
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState
import ru.herobrine1st.e621.ui.screen.post.component.PoolInfoCard
import ru.herobrine1st.e621.ui.screen.posts.component.HiddenItems
import ru.herobrine1st.e621.ui.screen.posts.component.Post
import ru.herobrine1st.e621.util.debug
import ru.herobrine1st.e621.util.isFavourite
import ru.herobrine1st.paging.api.LoadState
import ru.herobrine1st.paging.api.contentType
import ru.herobrine1st.paging.api.itemKey
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, CachedDataStore::class)
@Composable
fun Posts(
    screenSharedState: ScreenSharedState,
    component: PostListingComponent
) {
    val coroutineScope = rememberCoroutineScope()
    val favouritesCache by component.collectFavouritesCacheAsState()
    val lazyListState = rememberLazyListState()

    val posts = component.pagingItems

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.posts))
                },
                actions = {
                    debug {
                        val maxPosts by produceState(posts.size) {
                            snapshotFlow { posts.size }
                                .collect {
                                    value = max(value, it)
                                }
                        }
                        Text(
                            "Items in memory: ${posts.size}, max: $maxPosts",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    IconButton(onClick = {
                        component.onOpenSearch()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                    ActionBarMenu(
                        onNavigateToSettings = screenSharedState.goToSettings,
                        onOpenBlacklistDialog = screenSharedState.openBlacklistDialog
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = screenSharedState.snackbarHostState)
        }
    ) {
        val pullToRefreshState = rememberPullToRefreshState()
        val isLoading = posts.loadStates.refresh is LoadState.Loading

        // Working around M3 bug (PullToRefreshModifierNode only handles updates, not initial state)
        LaunchedEffect(pullToRefreshState) {
            if (isLoading) pullToRefreshState.animateToThreshold()
        }

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = posts::refresh,
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(it),
                    isRefreshing = isLoading,
                    state = pullToRefreshState
                )
            }
        ) {
            LazyColumn(
                state = lazyListState,
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = it,
                modifier = Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .fillMaxWidth()
            ) {
                when {
                    component.infoState is InfoState.Loading -> item {
                        Spacer(modifier = Modifier.height(4.dp))
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    posts.loadStates.prepend is LoadState.Error -> item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.unknown_error))
                        Button(onClick = { posts.retry() }) {
                            Text(stringResource(R.string.retry))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    posts.loadStates.prepend is LoadState.Complete &&
                            component.infoState is InfoState.PoolInfo -> item(
                        "PoolInfoCard",
                        contentType = "PoolInfoCard"
                    ) {
                        PoolInfoCard(
                            component.infoState as InfoState.PoolInfo,
                            modifier = Modifier.padding(
                                start = BASE_PADDING_HORIZONTAL,
                                end = BASE_PADDING_HORIZONTAL,
                                bottom = BASE_PADDING_HORIZONTAL,
                                top = 4.dp
                            )
                        )
                    }
                }

                if (posts.size == 0) item {
                    if (component.infoState is InfoState.None) Spacer(Modifier.height(4.dp))
                    when (posts.loadStates.refresh) {
                        is LoadState.Error -> {
                            Icon(Icons.Outlined.Error, contentDescription = null)
                            Text(stringResource(R.string.unknown_error))
                            Button(onClick = { posts.retry() }) {
                                Text(stringResource(R.string.retry))
                            }
                        }

                        LoadState.Complete -> Text(stringResource(R.string.empty_results))
                        LoadState.Loading -> {} // Nothing to do, PullRefreshIndicator already here
                        is LoadState.NotLoading, LoadState.Idle -> {}
                    }
                }

                items(
                    count = posts.size,
                    key = posts.itemKey { post -> post.key },
                    contentType = posts.contentType { post -> post.contentType }
                ) { index ->
                    // Due to first item with index zero being able to vanish scroll position
                    // resets every prepend when user sees at least a pixel of loading indicator or even its padding
                    // Moving it inside element which will not vanish resolves problem with scroll reset
                    // leaving only a small problem of snapping scroll to the size of loading indicator (i.e.
                    // posts are suddenly higher and user don't immediately see that loading is complete)
                    // This issue is fixed with scrollBy, leaving even more small problem that the problem
                    // described above it not fixed for one frame
                    // Likely onDispose is only called after the frame it is disposed
                    // So that we have one frame without indicator but when onDispose is not called yet
                    //
                    // The proper solution is to use NestedScroll and manually manage CircularProgressIndicator position based on it
                    // It is possible if changing contentPadding does not change scroll position, as it involves
                    // adding beforeContentPadding. Also custom vertical arrangement may help here. If it is not possible to remove beforeContentPadding
                    // without changing scroll position, this solution is doomed and will be equal to the solution below without DisposableEffect
                    //
                    // Other solutions:
                    // - Animate exit of CircularProgressIndicator, while also animating scroll
                    // - https://issuetracker.google.com/issues/273025639 may be useful if it allows negative offsets
                    //   (also fix for snapping due to big page sizes)
                    if (index == 0 && posts.loadStates.prepend is LoadState.Loading) {
                        var height by remember { mutableIntStateOf(-1) }
                        Column(Modifier.onSizeChanged { height = it.height }) {
                            Spacer(modifier = Modifier.height(4.dp))
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        DisposableEffect(Unit) {
                            onDispose {
                                // this lags for one frame
                                coroutineScope.launch {
                                    lazyListState.scrollBy(-height.toFloat())
                                }
                            }
                        }
                    }
                    val item = posts[index]
                    when (item) {
                        is UIPostListingItem.HiddenItemsBridge -> HiddenItems(item)

                        is UIPostListingItem.Post -> Post(
                            post = item.post,
                            favouriteState = favouritesCache.isFavourite(item.post),
                            isAuthorized = component.isAuthorized,
                            onFavouriteChange = {
                                component.handleFavouriteChange(item.post)
                            },
                            openPost = { openComments ->
                                component.onOpenPost(item.post, openComments)
                            },
                            onVote = {
                                component.vote(item.post.id, it)
                            },
                            getVote = {
                                component.getVote(item.post.id) ?: 0
                            }
                        )

                        is UIPostListingItem.Empty -> {}
                    }
                    if (item !is UIPostListingItem.Empty && index != posts.size - 1)
                        Spacer(Modifier.height(4.dp))

                }
                endOfPagePlaceholder(posts.loadStates.append, onRetry = posts::retry)
            }
        }
    }
}

