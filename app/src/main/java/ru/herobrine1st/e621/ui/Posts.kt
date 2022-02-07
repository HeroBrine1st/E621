package ru.herobrine1st.e621.ui

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.*
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.component.LazyBase
import ru.herobrine1st.e621.util.SearchOptions
import ru.herobrine1st.e621.util.lateinitMutableState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.withContext


private var vm: PostsViewModel by lateinitMutableState()

class PostsViewModel() : ViewModel() {

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
                applicationViewModel.fetchPosts(query, page = nextPage, limit = params.loadSize)
            }
            LoadResult.Page(
                data = posts,
                prevKey = if (nextPage == 1) null else nextPage - 1,
                nextKey = if (posts.isNotEmpty()) nextPage + 1 else null
            )
        } catch (e: Throwable) {
            Log.e("Posts", "Unable to load posts", e)
            LoadResult.Error(e)
        }
    }
}

@Composable
fun Posts(searchOptions: SearchOptions, applicationViewModel: ApplicationViewModel) {
    vm = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val postsFlow: Flow<PagingData<Post>> = remember(searchOptions) {
        Pager(PagingConfig(pageSize = 20)) {
            PostsSource(applicationViewModel, searchOptions)
        }.flow.cachedIn(coroutineScope)
    }
    val posts = postsFlow.collectAsLazyPagingItems()
    LazyBase {
        items(posts, key = { it.id }) { post ->
            Post(post!!)
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