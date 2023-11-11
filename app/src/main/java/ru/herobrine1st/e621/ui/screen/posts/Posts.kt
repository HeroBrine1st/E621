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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.navigation.component.posts.PostListingComponent
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.component.endOfPagePlaceholder
import ru.herobrine1st.e621.ui.component.post.PostActionRow
import ru.herobrine1st.e621.ui.component.post.PostMediaContainer
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState
import ru.herobrine1st.e621.util.FavouritesCache.FavouriteState
import ru.herobrine1st.e621.util.isFavourite
import ru.herobrine1st.e621.util.text

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun Posts(
    screenSharedState: ScreenSharedState,
    component: PostListingComponent,
    isAuthorized: Boolean, // TODO move to component
) {
    val posts = component.postsFlow.collectAsLazyPagingItems()
    val favouritesCache by component.collectFavouritesCacheAsState()
    val lazyListState = rememberLazyListState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = posts.loadState.refresh is LoadState.Loading,
        // TODO move away from Jetpack Paging: cannot refresh from out of composable
        // also I want to manipulate on the items in a way like it can be done with a regular list
        // (I want to show how many posts are skipped due to blacklist, like hidden items on github)
        onRefresh = { posts.refresh() }
    )
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
                .pullRefresh(pullRefreshState)
                .fillMaxSize()
        ) {
            LazyColumn(
                // Solution from https://issuetracker.google.com/issues/177245496#comment24
                state = if (posts.itemCount == 0) rememberLazyListState() else lazyListState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                endOfPagePlaceholder(posts.loadState.prepend)
                // TODO add info about pool here, getting that info from component
                if (posts.itemCount == 0) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(BASE_PADDING_HORIZONTAL)
                                .fillMaxSize()
                        ) {
                            when (posts.loadState.refresh) {
                                is LoadState.NotLoading -> Text(stringResource(R.string.empty_results))
                                is LoadState.Error -> {
                                    Icon(Icons.Outlined.Error, contentDescription = null)
                                    Text(stringResource(R.string.unknown_error))
                                }

                                LoadState.Loading -> {} // Nothing to do, PullRefreshIndicator already here
                            }
                        }
                    }
                }
                items(
                    count = posts.itemCount,
                    key = posts.itemKey { post -> post.id },
                    // contentType is purposely ignored as all items are of the same type and additional calls to Paging library are not needed
                ) { index ->
                    val post = posts[index] ?: return@items
                    Post(
                        post = post,
                        favouriteState = favouritesCache.isFavourite(post),
                        isAuthorized = isAuthorized,
                        onFavouriteChange = {
                            component.handleFavouriteChange(post)
                        },
                        openPost = { openComments ->
                            component.onOpenPost(post, openComments)
                        }
                    )

                }
                endOfPagePlaceholder(posts.loadState.append)
            }
            // FIXME indicator is shown for a moment after navigating back
            // Related: https://issuetracker.google.com/issues/177245496
            // (but the actual cause is absence of a "None" state like in Coil, which indicates
            // that no request is in fly but no data available hence no loading and no indicator)
            PullRefreshIndicator(
                refreshing = posts.loadState.refresh is LoadState.Loading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Post(
    post: Post,
    favouriteState: FavouriteState,
    isAuthorized: Boolean,
    onFavouriteChange: () -> Unit,
    openPost: (scrollToComments: Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (post.normalizedSample.type.isVideo) {
                Text(
                    stringResource(
                        R.string.assertion_failed,
                        "API_RETURNED_VIDEO_SAMPLE_${post.id}"
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
            Divider(Modifier.padding(horizontal = 8.dp))
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

