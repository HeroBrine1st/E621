package ru.herobrine1st.e621.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.screen.Screens
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.LazyBase
import ru.herobrine1st.e621.util.SearchOptions
import java.io.IOException

class PostsViewModel(
    private val applicationViewModel: ApplicationViewModel, // will not change
    private val searchOptions: SearchOptions // will not change
) : ViewModel() {
    val postsFlow: Flow<PagingData<Post>> =
        Pager(PagingConfig(pageSize = 20)) {
            PostsSource(applicationViewModel, searchOptions)
        }.flow.cachedIn(viewModelScope)
    val lazyListState = LazyListState(0, 0)

    init {
        Log.d("PostsViewModel", "Created new PostsViewModel")
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

val PostsAppBarActions: @Composable RowScope.(NavHostController) -> Unit = { navController ->
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
            contentDescription = stringResource(R.string.search)
        )
    }
}

@Composable
fun Post(post: Post) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(post.tags.artist.joinToString(" "))
        }
    }
}

class PostsSource(
    private val applicationViewModel: ApplicationViewModel,
    searchOptions: SearchOptions
) : PagingSource<Int, Post>() {
    private val query = searchOptions.compileToQuery()
    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        return try {
            val nextPage = params.key ?: 1
            val posts: List<Post> = withContext(Dispatchers.IO) {
                Api.getPosts(query, page = nextPage, limit = params.loadSize)
            }
            LoadResult.Page(
                data = posts,
                prevKey = if (nextPage == 1) null else nextPage - 1,
                nextKey = if (posts.isNotEmpty()) nextPage + 1 else null
            )
        } catch (e: IOException) {
            applicationViewModel.addSnackbarMessage(
                R.string.network_error,
                SnackbarDuration.Indefinite
            )
            LoadResult.Error(e)
        } catch (e: Throwable) {
            Log.e("Posts", "Unable to load posts", e)
            LoadResult.Error(e)
        }
    }
}

@Composable
fun Posts(searchOptions: SearchOptions, applicationViewModel: ApplicationViewModel) {
    val viewModel: PostsViewModel =
        viewModel(factory = PostsViewModelFactory(applicationViewModel, searchOptions))
    val posts = viewModel.postsFlow.collectAsLazyPagingItems()
    LaunchedEffect(true) {
        Log.d(
            "Posts",
            "Scroll state: ${viewModel.lazyListState.firstVisibleItemIndex} ${viewModel.lazyListState.firstVisibleItemScrollOffset}"
        )
    }
    if (posts.itemCount == 0) { // Do not reset lazyListState
        Base(Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(4.dp))
            CircularProgressIndicator()
        }
        return
    }
    LazyBase(
        lazyListState = viewModel.lazyListState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }
        items(posts, key = { it.id }) { post ->
            Post(post!!)
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