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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.navigation.component.posts.PostListingComponent
import ru.herobrine1st.e621.navigation.component.posts.PostListingItem
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.component.endOfPagePlaceholder
import ru.herobrine1st.e621.ui.component.post.PostActionRow
import ru.herobrine1st.e621.ui.component.post.PostMediaContainer
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState
import ru.herobrine1st.e621.util.FavouritesCache.FavouriteState
import ru.herobrine1st.e621.util.isFavourite
import ru.herobrine1st.e621.util.text
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

    var index by remember { mutableIntStateOf(-1) }
    val posts = remember {
        component.postsFlow.correctFirstVisibleItem(
            getFirstVisibleItemIndex = { lazyListState.firstVisibleItemIndex },
            setIndex = { index = it }
        )
    }
        .collectAsPagingItems(startImmediately = true)

    LaunchedEffect(key1 = index) {
        if (index == -1) return@LaunchedEffect
        lazyListState.scrollToItem(index)
        index = -1
    }

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
    //       and calls refresh()
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
                .padding(it)
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection) // TODO probably box is redundant
        ) {
            LazyColumn(
                // Solution from https://issuetracker.google.com/issues/177245496#comment24
                state = if (posts.size == 0) rememberLazyListState() else lazyListState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                endOfPagePlaceholder(posts.loadStates.prepend)
                // TODO add info about pool here, getting that info from component
                if (posts.size == 0) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(BASE_PADDING_HORIZONTAL)
                                .fillMaxSize()
                        ) {
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
                    }
                }
                items(
                    count = posts.size,
                    key = posts.itemKey { post -> post.key },
                    contentType = posts.contentType { post -> post.contentType }
                ) { index ->
                    when (val item = posts[index]) {
                        is PostListingItem.HiddenItems -> {
                            val modifier = Modifier.padding(horizontal = 8.dp)
                            if (item.hiddenDueToSafeModeNumber == 0) {
                                Text(
                                    stringResource(
                                        R.string.posts_hidden_blacklisted,
                                        pluralStringResource(
                                            id = R.plurals.list_items,
                                            count = item.hiddenDueToBlacklistNumber,
                                            item.hiddenDueToBlacklistNumber
                                        )
                                    ), modifier
                                )
                            } else if (item.hiddenDueToBlacklistNumber == 0) {
                                Text(
                                    stringResource(
                                        R.string.posts_hidden_safe_mode,
                                        pluralStringResource(
                                            id = R.plurals.list_items,
                                            count = item.hiddenDueToSafeModeNumber,
                                            item.hiddenDueToSafeModeNumber
                                        )
                                    ), modifier
                                )
                            } else {
                                Text(
                                    stringResource(
                                        R.string.posts_hidden_blacklist_and_safe_mode,
                                        pluralStringResource(
                                            id = R.plurals.list_items,
                                            count = item.hiddenDueToSafeModeNumber,
                                            item.hiddenDueToSafeModeNumber
                                        ),
                                        item.hiddenDueToBlacklistNumber
                                    ), modifier
                                )
                            }
                        }

                        is PostListingItem.Post -> Post(
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
                    }

                }
                endOfPagePlaceholder(posts.loadStates.append)
            }
            // FIXME indicator is shown for a moment after navigating back
            // Related: https://issuetracker.google.com/issues/177245496
            // (but the actual cause is absence of a "None" state like in Coil, which indicates
            // that no request is in fly but no data available hence no loading and no indicator)
//            PullRefreshIndicator(
//                refreshing = posts.loadState.refresh is LoadState.Loading,
//                state = pullRefreshState,
//                modifier = Modifier.align(Alignment.TopCenter)
//            )

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Post(
    post: Post,
    favouriteState: FavouriteState,
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
            // FIXME UI jank in both FlowRow and PostActionsRow
            // This issue is somehow related to Text, but quick test shows that removing Text
            // does not help while removing both FlowRow and PostActionsRow make scrolling smooth
            // even in debug build.
            //
            // First, this was only visible in decomposition, but a day or two after it came into
            // composition as well, and then someday vanished from decomposition. This happened
            // literally while I was bisecting it.
            // Also someday I somehow found that Text is source of jank, but now it is not. I don't
            // remember reproduce steps. Copying tags (literally six pointers to strings) also is
            // not the source, as I have tested like two weeks ago.
            // And PostActionsRow even does not have any state, it literally use what is given.
            // What the fucking fuck.
            // Possible source: my device? Upstream? Idk.
            //
            // Btw, in release build this issue is unnoticeable when you don't know it is there.
            // It has been there forever (literally from february 2022) and I noticed it only
            // in december while optimizing blacklist.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                // TODO crossAxisSpacing = 2.dp,
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

