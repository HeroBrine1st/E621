package ru.herobrine1st.e621.ui.screen.posts

import android.app.Activity
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.paging.compose.items
import com.google.accompanist.flowlayout.FlowRow
import dagger.hilt.android.EntryPointAccessors
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.OutlinedChip
import ru.herobrine1st.e621.ui.screen.Screen
import ru.herobrine1st.e621.ui.screen.posts.logic.PostsViewModel
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor
import ru.herobrine1st.e621.util.PostsSearchOptions
import ru.herobrine1st.e621.util.SearchOptions

@Composable
fun PostsAppBarActions(navController: NavHostController) {
    IconButton(onClick = {
        val arguments = navController.currentBackStackEntry!!.arguments!!
        navController.navigate(
            Screen.Search.buildRoute {
                addArgument("query", arguments.getParcelable<PostsSearchOptions>("query")!!)
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
    isBlacklistEnabled: Boolean,
    openPost: (post: Post, scrollToComments: Boolean) -> Unit,
    viewModel: PostsViewModel = viewModel(
        factory = PostsViewModel.provideFactory(
            EntryPointAccessors.fromActivity(
                LocalContext.current as Activity,
                PostsViewModel.FactoryProvider::class.java
            ).provideFactory(), searchOptions
        )
    )
) {
    val isAuthorized by viewModel.isAuthorizedFlow.collectAsState(false)
    val posts = viewModel.postsFlow.collectAsLazyPagingItems()


    if (posts.loadState.refresh !is LoadState.NotLoading // Do not reset lazyListState
        || viewModel.isBlacklistLoading
    ) {
        Base {
            Spacer(modifier = Modifier.height(4.dp))
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        state = viewModel.lazyListState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        endOfPagePlaceholder(posts.loadState.prepend)
        items(posts, key = { it.id }) { post ->
            if (post == null) return@items
            val blacklisted = isBlacklistEnabled && viewModel.isHiddenByBlacklist(post)
            LaunchedEffect(blacklisted) { viewModel.notifyPostState(blacklisted) }
            if (blacklisted) return@items
            Post(
                post = post,
                isFavourite = viewModel.isFavourite(post),
                isAuthorized = isAuthorized,
                onAddToFavourites = {
                    viewModel.handleFavouriteButtonClick(post)
                }
            ) { scrollToComments -> openPost(post, scrollToComments) }
            Spacer(modifier = Modifier.height(4.dp))
        }
        endOfPagePlaceholder(posts.loadState.append)
    }
}

@Composable
fun Post(
    post: Post,
    isFavourite: Boolean,
    isAuthorized: Boolean,
    onAddToFavourites: () -> Unit,
    openPost: (scrollToComments: Boolean) -> Unit
) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            when {
                post.file.type.isSupported -> PostImage(
                    post = post,
                    aspectRatio = post.normalizedSample.aspectRatio,
                    openPost = openPost,
                    file = post.normalizedSample
                )
                else -> InvalidPost(text = stringResource(R.string.unsupported_post_type))
            }
            FlowRow {
                var expandTags by remember { mutableStateOf(false) }
                post.tags.reduced
                    .let {
                        if (expandTags) it
                        else it.take(6)
                    }
                    .forEach {
                        OutlinedChip(
                            modifier = Modifier.padding(
                                horizontal = 4.dp,
                                vertical = 2.dp
                            )
                        ) {
                            Text(it, style = MaterialTheme.typography.caption)
                        }
                    }
                if (!expandTags && post.tags.reduced.size > 6) {
                    OutlinedChip(modifier = Modifier
                        .padding(4.dp)
                        .clickable {
                            expandTags = true
                        }) {
                        Text("...", style = MaterialTheme.typography.caption)
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Divider()
                PostActionsRow(post, isFavourite, isAuthorized, onAddToFavourites) {
                    openPost(true)
                }
                Text("Created ${DateUtils.getRelativeTimeSpanString(post.createdAt.toEpochSecond() * 1000)}") // TODO i18n; move it somewhere
            }
        }
    }
}

// edge of page, start and end of page or anything, it just doesn't matter while the name is clear
fun LazyListScope.endOfPagePlaceholder(loadState: LoadState) {
    when (loadState) {
        is LoadState.Loading -> {
            item {
                Base {
                    Spacer(modifier = Modifier.height(4.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        is LoadState.Error -> {
            item {
                Base {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("error")
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        else -> {}
    }
}