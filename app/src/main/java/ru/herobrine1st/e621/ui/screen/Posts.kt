package ru.herobrine1st.e621.ui.screen

import android.text.format.DateUtils
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.rememberImagePainter
import com.google.accompanist.flowlayout.FlowRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.AuthState
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.preference.BLACKLIST_ENABLED
import ru.herobrine1st.e621.preference.getPreference
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.OutlinedChip
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor
import ru.herobrine1st.e621.util.SearchOptions
import java.io.IOException

class PostsViewModel(
    private val applicationViewModel: ApplicationViewModel, // will not change
    private val searchOptions: SearchOptions, // will not change
    pageSize: Int = 100
) : ViewModel() {
    private val pager = Pager(PagingConfig(pageSize = pageSize, initialLoadSize = pageSize)) {
        PostsSource(applicationViewModel, searchOptions)
    }

    val postsFlow: Flow<PagingData<Post>> = pager.flow.cachedIn(viewModelScope)

    val lazyListState = LazyListState(0, 0)

    private var countBlacklistedPosts = 0
    private var warnedUser = false

    /**
     * Very primitive detection of intersection between query and blacklist. It simply warns user about it.
     *
     * @param blacklisted result of [ApplicationViewModel.blacklistPostPredicate] if blacklist is enabled, otherwise false
     */
    fun notifyPostState(blacklisted: Boolean) {
        if (!blacklisted) countBlacklistedPosts = 0 else countBlacklistedPosts++
        if (countBlacklistedPosts > 300 && !warnedUser) {
            warnedUser = true
            Log.i("PostsViewModel", "Detected intersection between blacklist and query")
            applicationViewModel.addSnackbarMessage(
                R.string.maybe_search_query_intersects_with_blacklist,
                SnackbarDuration.Indefinite
            )
        }
    }
}

class PostsViewModelFactory(
    private val applicationViewModel: ApplicationViewModel, // will not change
    private val searchOptions: SearchOptions // will not change
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PostsViewModel(applicationViewModel, searchOptions) as T
    }
}

@Composable
fun PostsAppBarActions(navController: NavHostController) {
    IconButton(onClick = {
        val arguments = navController.currentBackStackEntry!!.arguments!!
        navController.navigate(
            Screens.Search.buildRoute {
                addArgument("tags", arguments.getString("tags"))
                addArgument("order", arguments.getString("order"))
                addArgument("ascending", arguments.getBoolean("ascending"))
                addArgument("rating", arguments.getString("rating"))
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

class PostsSource(
    private val applicationViewModel: ApplicationViewModel,
    searchOptions: SearchOptions,
) : PagingSource<Int, Post>() {
    private val query = searchOptions.compileToQuery()
    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.div(state.config.pageSize)?.plus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        return try {
            val page = params.key ?: 1
            val posts: List<Post> = withContext(Dispatchers.IO) {
                applicationViewModel.api.getPosts(query, page = page, limit = params.loadSize)
            }
            LoadResult.Page(
                data = posts,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (posts.isNotEmpty()) page + 1 else null
            )
        } catch (e: IOException) {
            applicationViewModel.addSnackbarMessage(
                R.string.network_error,
                SnackbarDuration.Indefinite
            )
            Log.e("Posts", "Unable to load posts", e)
            LoadResult.Error(e)
        } catch (e: Throwable) {
            Log.e("Posts", "Unable to load posts", e)
            LoadResult.Error(e)
        }
    }
}

@Composable
fun Posts(
    searchOptions: SearchOptions,
    applicationViewModel: ApplicationViewModel,
    openPost: (id: Int, scrollToComments: Boolean) -> Unit
) {
    val viewModel: PostsViewModel =
        viewModel(factory = PostsViewModelFactory(applicationViewModel, searchOptions))
    val posts = viewModel.postsFlow.collectAsLazyPagingItems()
    val blacklistEnabled =
        LocalContext.current.getPreference(key = BLACKLIST_ENABLED, defaultValue = true)

    if (posts.itemCount == 0) { // Do not reset lazyListState
        Base(Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(4.dp))
            CircularProgressIndicator()
        }
        return
    }
    LazyColumn(
        state = viewModel.lazyListState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        items(posts, key = { it.id }) { post ->
            if (post == null) return@items
            val blacklisted =
                blacklistEnabled && applicationViewModel.blacklistPostPredicate.test(post)
            viewModel.notifyPostState(blacklisted)
            if (blacklisted) return@items
            Post(post, applicationViewModel) {
                openPost(post.id, it)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        posts.apply {
            when {
                loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading -> {
                    item { CircularProgressIndicator() }
                }
                loadState.refresh is LoadState.Error || loadState.append is LoadState.Error -> {
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
    applicationViewModel: ApplicationViewModel,
    openPost: (scrollToComments: Boolean) -> Unit
) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            PostImagePreview(post)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(
                                Icons.Filled.ArrowUpward,
                                contentDescription = stringResource(R.string.score_up)
                            )
                        }
                        Text(post.score.total.toString())
                        IconButton(onClick = { /*TODO*/ }) {
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
                        ) { openPost(true) }
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
                    if (applicationViewModel.authState == AuthState.AUTHORIZED) {
                        val isFavorited =
                            applicationViewModel.isFavorited(post.id, post.isFavorited)
                        IconButton(onClick = {
                            applicationViewModel.handleFavoritePost(post)
                        }) {
                            Crossfade(targetState = isFavorited) {
                                if (it) Icon(
                                    Icons.Filled.Favorite,
                                    contentDescription = stringResource(R.string.unfavorite)
                                ) else Icon(
                                    Icons.Filled.FavoriteBorder,
                                    contentDescription = stringResource(R.string.favorite)
                                )
                            }
                        }
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.share)
                        )
                    }
                }
                Text("Created ${DateUtils.getRelativeTimeSpanString(post.createdAt.toEpochMilli())}") // TODO i18n; move it somewhere
            }
        }
    }
}

@Composable
fun PostImagePreview(post: Post) {
    val aspectRatio = post.sample.width.toFloat() / post.sample.height.toFloat()
    if (aspectRatio <= 0) {
        Box(contentAlignment = Alignment.TopCenter) {
            Text("Invalid post") // TODO i18n
        }
        return
    }
    Box(contentAlignment = Alignment.TopStart) {
        Image(
            painter = rememberImagePainter(
                post.sample.url,
                builder = {
                    crossfade(true)
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
            contentDescription = remember(post.id) { post.tags.all.joinToString(" ") }
        )
        if (post.file.type.isNotImage) OutlinedChip( // TODO
            modifier = Modifier.offset(x = 10.dp, y = 10.dp),
            backgroundColor = Color.Transparent
        ) {
            Text(post.file.type.extension)
        }
    }
}