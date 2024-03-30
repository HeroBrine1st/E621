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

package ru.herobrine1st.e621.ui.screen.posts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import ru.herobrine1st.e621.R
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
import ru.herobrine1st.e621.util.isFavourite
import ru.herobrine1st.paging.api.LoadState
import ru.herobrine1st.paging.api.collectAsPagingItems
import ru.herobrine1st.paging.api.contentType
import ru.herobrine1st.paging.api.itemKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Posts(
    screenSharedState: ScreenSharedState,
    component: PostListingComponent,
    isAuthorized: Boolean, // TODO move to component
) {
    val favouritesCache by component.collectFavouritesCacheAsState()
    val lazyListState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()

    val posts = component.postsFlow.collectAsPagingItems(startImmediately = true)

    //region Working around strange API
    // https://issuetracker.google.com/issues/317177683
    // New API is strange. Why don't just use previous interfaces: state of refreshing from
    // user code and callback to refresh from library?
    // Now I need to connect pullToRefreshState.isRefreshing to posts.loadState.refresh is LoadState.Loading
    // Both are data, neither is callback
    if (pullToRefreshState.isRefreshing && posts.loadStates.refresh !is LoadState.Loading) {
        LaunchedEffect(Unit) {
            posts.refresh()
        }
    }

    // And we have first design-based issue just after 22 days of this code, congratulations!
    // FIXME if LaunchedEffect below leaves composition while condition is true,
    //       there's nothing that will call endRefresh()
    //       which in turn can make condition above true
    //       and call refresh() again
    // Steps to reproduce:
    // 1. Enter this screen
    // 2. Immediately go into search or settings
    // 3. Wait until request completes (look to logs)
    // 4. Return back - it will do the same request again
    if (posts.loadStates.refresh is LoadState.Loading) {
        LaunchedEffect(Unit) {
            pullToRefreshState.startRefresh()
            // wait until it completes
            snapshotFlow { posts.loadStates.refresh is LoadState.Loading }
                .first { !it }
            pullToRefreshState.endRefresh()
        }
    }
    //endregion

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.posts))
                },
                actions = {
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
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            LazyColumn(
                // Solution from https://issuetracker.google.com/issues/177245496#comment24
                state = if (posts.size == 0) rememberLazyListState() else lazyListState,
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = it
            ) {
                when {
                    posts.loadStates.prepend is LoadState.Loading ||
                            component.infoState is InfoState.Loading -> item {
                        Spacer(modifier = Modifier.height(4.dp))
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    posts.loadStates.prepend is LoadState.Error -> item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.unknown_error))
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
                    val item = posts[index]
                    when (item) {
                        is UIPostListingItem.HiddenItemsBridge -> HiddenItems(item)

                        is UIPostListingItem.Post -> Post(
                            post = item.post,
                            favouriteState = favouritesCache.isFavourite(item.post),
                            isAuthorized = isAuthorized,
                            onFavouriteChange = {
                                component.handleFavouriteChange(item.post)
                            },
                            openPost = { openComments ->
                                component.onOpenPost(item.post, openComments)
                            }
                        )

                        is UIPostListingItem.Empty -> {}
                    }
                    if (item !is UIPostListingItem.Empty && index != posts.size - 1)
                        Spacer(Modifier.height(4.dp))

                }
                endOfPagePlaceholder(posts.loadStates.append)
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier
                    .padding(it)
                    .align(Alignment.TopCenter)
            )
        }
    }
}

