package ru.herobrine1st.e621.ui.screen.posts

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.flowlayout.FlowRow
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.OutlinedChip
import ru.herobrine1st.e621.ui.screen.Screen
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
    isFavourite: (Post) -> Boolean,
    isHiddenByBlacklist: (Post) -> Boolean,
    isAuthorized: Boolean,
    onAddToFavourites: (Post) -> Unit,
    openPost: (post: Post, scrollToComments: Boolean) -> Unit
) {
    val viewModel: PostsViewModel = hiltViewModel()

    val posts = viewModel.postsFlow.collectAsLazyPagingItems()
    val loadState = posts.loadState

    val loading = loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading
    val error = loadState.refresh is LoadState.Error || loadState.append is LoadState.Error

    LaunchedEffect(searchOptions) {
        viewModel.onSearchOptionsChange(searchOptions) {
            posts.refresh()
        }
    }

    if (posts.itemCount == 0 && !error) { // Do not reset lazyListState
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
        items(posts, key = { it.id }) { post ->
            if (post == null) return@items
            val blacklisted = isHiddenByBlacklist(post)
            viewModel.notifyPostState(blacklisted)
            if (blacklisted) return@items
            Post(
                post = post,
                isFavourite = isFavourite(post),
                isAuthorized = isAuthorized,
                onAddToFavourites = {
                    onAddToFavourites(post)
                }
            ) { scrollToComments -> openPost(post, scrollToComments) }
            Spacer(modifier = Modifier.height(4.dp))
        }
        posts.apply {
            when {
                loading -> {
                    item {
                        Base {
                            Spacer(modifier = Modifier.height(4.dp))
                            CircularProgressIndicator()
                        }
                    }
                }
                error -> {
                    item {
                        Text("error")
                    }
                }
            }
        }
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