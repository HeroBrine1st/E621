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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.module.CachedDataStore
import ru.herobrine1st.e621.navigation.component.posts.PostListingComponent
import ru.herobrine1st.e621.navigation.component.posts.PostListingComponent.InfoState
import ru.herobrine1st.e621.navigation.component.posts.UIPostListingItem
import ru.herobrine1st.e621.preference.Preferences
import ru.herobrine1st.e621.ui.component.endOfPagePlaceholder
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState
import ru.herobrine1st.e621.ui.screen.post.component.PoolInfoCard
import ru.herobrine1st.e621.ui.screen.posts.PostsScreenDefaults.EDGE_PADDING
import ru.herobrine1st.e621.ui.screen.posts.PostsScreenDefaults.HORIZONTAL_SPACING
import ru.herobrine1st.e621.ui.screen.posts.PostsScreenDefaults.VERTICAL_SPACING
import ru.herobrine1st.e621.ui.screen.posts.component.HiddenItems
import ru.herobrine1st.e621.ui.screen.posts.component.Post
import ru.herobrine1st.e621.util.debug
import ru.herobrine1st.e621.util.isFavourite
import ru.herobrine1st.paging.api.LoadState
import ru.herobrine1st.paging.api.contentType
import ru.herobrine1st.paging.api.itemKey
import kotlin.math.max

object PostsScreenDefaults {
    val EDGE_PADDING = 4.dp
    val HORIZONTAL_SPACING = 4.dp
    val VERTICAL_SPACING = 4.dp
}

@OptIn(ExperimentalMaterial3Api::class, CachedDataStore::class)
@Composable
fun Posts(
    screenSharedState: ScreenSharedState,
    component: PostListingComponent
) {
    val favouritesCache by component.collectFavouritesCacheAsState()
    val gridState = rememberLazyStaggeredGridState()

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
    ) { paddingValues ->
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
                        .padding(paddingValues),
                    isRefreshing = isLoading,
                    state = pullToRefreshState
                )
            }
        ) {
            val columns = when (val layout = component.layoutPreference) {
                is Preferences.PostsColumns.Adaptive -> StaggeredGridCells.Adaptive(layout.widthDp.dp)
                is Preferences.PostsColumns.Fixed -> StaggeredGridCells.Fixed(layout.columnCount)
            }

            val density = LocalDensity.current
            // avoiding SubcomposeLayout because one is already there
            var lazyListConstraints by remember { mutableStateOf(Constraints()) }
            val columnCount by remember {
                derivedStateOf {
                    if (!lazyListConstraints.hasBoundedWidth) error("lazyListConstraints is infinite! Did you forget to set it?")
                    with(columns) {
                        with(density) {
                            density.calculateCrossAxisCellSizes(
                                lazyListConstraints.maxWidth - EDGE_PADDING.roundToPx(),
                                HORIZONTAL_SPACING.roundToPx()
                            ).size
                        }
                    }
                }
            }

            var recentFirstKey by remember { mutableStateOf<Any?>(null) }
            LazyVerticalStaggeredGrid(
                columns = columns,
                state = gridState,
                // all non-full-lane elements use edge padding, using negative spacing to remove extra padding
                horizontalArrangement = Arrangement.spacedBy(HORIZONTAL_SPACING - EDGE_PADDING * 2),
                contentPadding = paddingValues,
                modifier = Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .fillMaxWidth()
                    // This layout modifier blocks runs on both measurement due to page append/prepend
                    // and also due to scroll
                    // As scroll is asynchronous, this modifier holds the scroll until our code is complete
                    // SideEffect doesn't fire on scroll, and this fires. That is.
                    .layout { measurable, constraints ->
                        // it's a shame there's no analogue to SideEffect here, but I think layout phase can't be interrupted
                        // if it can, then we can simply remove this optimization and read layoutInfo every time
                        val firstKey = posts.items.firstOrNull()?.key
                        if (firstKey != recentFirstKey) {
                            recentFirstKey = firstKey
                            val oldFirstItem =
                                gridState.layoutInfo.visibleItemsInfo.firstOrNull()
                            if (oldFirstItem != null) {
                                // TODO handle infoState and other items before post listing
                                if (posts.items.getOrNull(oldFirstItem.index)?.key != oldFirstItem.key) {
                                    val newIndex =
                                        posts.items.indexOfFirst { it.key == oldFirstItem.key }
                                    if (newIndex != -1)
                                        gridState.requestScrollToItem(
                                            newIndex,
                                            gridState.firstVisibleItemScrollOffset
                                        )
                                }
                            }
                        }
                        // Also save latest constraints so it can be used to calculate column size
                        // SAFETY: Content lambda is called on measure call
                        lazyListConstraints = constraints
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            ) {
                when {
                    component.infoState is InfoState.Loading -> item(span = StaggeredGridItemSpan.FullLine) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(VERTICAL_SPACING))
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(VERTICAL_SPACING))
                        }
                    }

                    posts.loadStates.prepend is LoadState.Error -> item(span = StaggeredGridItemSpan.FullLine) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(VERTICAL_SPACING))
                            Text(stringResource(R.string.unknown_error))
                            Button(onClick = { posts.retry() }) {
                                Text(stringResource(R.string.retry))
                            }
                            Spacer(modifier = Modifier.height(VERTICAL_SPACING))
                        }
                    }

                    (posts.loadStates.prepend is LoadState.Complete || posts.loadStates.prepend is LoadState.Idle) &&
                            component.infoState is InfoState.PoolInfo -> item(
                        "PoolInfoCard",
                        contentType = "PoolInfoCard",
                        span = StaggeredGridItemSpan.FullLine
                    ) {
                        PoolInfoCard(
                            component.infoState as InfoState.PoolInfo,
                            modifier = Modifier.padding(
                                vertical = VERTICAL_SPACING,
                                horizontal = EDGE_PADDING
                            )
                        )
                    }
                }

                if (posts.size == 0) item(span = StaggeredGridItemSpan.FullLine) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (component.infoState is InfoState.None) Spacer(Modifier.height(VERTICAL_SPACING))
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
                }

                val spanByIndex: (Int) -> StaggeredGridItemSpan = { index ->
                    if (columnCount == 1) StaggeredGridItemSpan.FullLine
                    else when (posts.items[index]) {
                        is UIPostListingItem.Empty -> StaggeredGridItemSpan.FullLine // using the fact that it always follows HiddenItemsBridge
                        is UIPostListingItem.HiddenItemsBridge -> StaggeredGridItemSpan.FullLine
                        is UIPostListingItem.Post -> {
                            // find the start of current column
                            val startingIndex = (posts.items.subList(0, index).indexOfLast { item ->
                                item is UIPostListingItem.Empty || item is UIPostListingItem.HiddenItemsBridge
                            } + 1)
                            val rowStartingIndex = index - ((index - startingIndex) % columnCount)

                            when {
                                // Not enough items to build full column
                                posts.items.size < rowStartingIndex + columnCount -> StaggeredGridItemSpan.FullLine

                                // Not enough items to build full column but this time a delimiter is found
                                posts.items.drop(rowStartingIndex).take(columnCount).any { item ->
                                    item is UIPostListingItem.Empty || item is UIPostListingItem.HiddenItemsBridge
                                } -> StaggeredGridItemSpan.FullLine

                                // Normal
                                else -> StaggeredGridItemSpan.SingleLane
                            }
                        }
                    }
                }


                items(
                    count = posts.size,
                    key = posts.itemKey { post -> post.key },
                    contentType = posts.contentType { post -> post.contentType },
                    span = spanByIndex
                ) { index ->
                    val item = posts[index]
                    if (index == 0 && posts.loadStates.prepend is LoadState.Loading) {
                        // TODO move it all into layout modifier
                        var height by remember { mutableIntStateOf(-1) }
                        Column(Modifier.onSizeChanged { height = it.height }) {
                            Spacer(modifier = Modifier.height(VERTICAL_SPACING))
                            // It normally should be a first item
                            // However, when it vanishes and new posts are prepended, LazyList maintains position by index, as prepend indicator is removed
                            // Its index is 0. At index 0 there's an item from a new page, leading to scroll jump.
                            // There, prefetch is triggered again (as user can't scroll back anymore due to no previous items), and everything repeats
                            // By placing indicator inside first post, we avoid that
                            // Size change due to vanishing of this indicator is compensated below.
                            // Size change due to appearance of this indicator isn't compensated due to those assumptions:
                            // - Prepending is started long before user actually reaches this item
                            // - Prepending is started due to retry, and retry button has "natural" snapping to this indicator
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(VERTICAL_SPACING))
                        }
                        DisposableEffect(Unit) {
                            onDispose { // called in applier thread
                                val firstVisibleItem =
                                    gridState.layoutInfo.visibleItemsInfo.firstOrNull()
                                // if user simply scrolled away, don't do anything
                                if (firstVisibleItem?.key != item.key) return@onDispose
                                // otherwise we compensate size change (due to CircularProgressIndicator being disposed) with scrolling back by that size change

                                // SAFETY: There is a race condition. Let's assume each case:
                                // - No scroll is performed. firstVisibleItem.index doesn't correlate to its real position now, and we override that correctly.
                                //   Offset is consistent as no scroll is performed.
                                // - Concurrent scroll is performed, then it is a race condition. firstVisibleItem is fully updated to accommodate that and
                                //   offset is updated to new value, which we use here. firstVisibleItem.index being updated can be used to optimize this code.
                                // This doesn't guarantee anything, but I think it is reasonable enough to be safe.
                                gridState.requestScrollToItem(
                                    posts.items.indexOfFirst { it.key == item.key },
                                    firstVisibleItem.offset.y - height
                                )
                            }
                        }
                    }
                    val span = spanByIndex(index)
                    Column(
                        // horizontal padding 4 dp will be subtracted between elements
                        modifier = Modifier.run {
                            if (span == StaggeredGridItemSpan.SingleLane) padding(horizontal = EDGE_PADDING)
                            else this
                        }
                    ) {
                        when (item) {
                            is UIPostListingItem.HiddenItemsBridge -> Row {
                                Spacer(Modifier.weight(1f))
                                HiddenItems(item)
                                Spacer(Modifier.weight(1f))
                            }

                            is UIPostListingItem.Post -> Post(
                                post = item.post,
                                favouriteState = favouritesCache.isFavourite(item.post),
                                isAuthorized = component.isAuthorized,
                                onFavouriteChange = {
                                    component.handleFavouriteChange(item)
                                },
                                openPost = { openComments ->
                                    component.onOpenPost(item, openComments)
                                },
                                onVote = {
                                    component.vote(item, it)
                                },
                                getVote = {
                                    component.getVote(item) ?: 0
                                },
                                hideChrome = span == StaggeredGridItemSpan.SingleLane
                            )

                            // TODO remove Empty, use layout modifier to handle blacklist state change
                            //      and move spacer below to verticalItemSpacing in grid
                            is UIPostListingItem.Empty -> {}
                        }
                        if (item !is UIPostListingItem.Empty && index != posts.size - 1)
                            Spacer(Modifier.height(VERTICAL_SPACING))
                    }

                }
                endOfPagePlaceholder(posts.loadStates.append, onRetry = posts::retry)
            }
        }
    }
}

