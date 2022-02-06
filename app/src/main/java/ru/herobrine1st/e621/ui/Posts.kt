package ru.herobrine1st.e621.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.widget.ContentLoadingProgressBar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.LazyBase
import ru.herobrine1st.e621.util.SearchOptions
import ru.herobrine1st.e621.util.lateinitMutableState

private var vm: PostsViewModel by lateinitMutableState()

class PostsViewModel : ViewModel() {

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

@Composable
fun Posts(searchOptions: SearchOptions, applicationViewModel: ApplicationViewModel) {
    vm = viewModel()
    val coroutineScope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    val posts: List<Post> by produceState(initialValue = emptyList()) {
        coroutineScope.launch(Dispatchers.IO) {
            loading = true
            value = applicationViewModel.fetchPosts(searchOptions.compileToQuery())
            loading = false
        }
    }
    val lazyListState = rememberLazyListState()
    LazyBase(lazyListState) {
        itemsIndexed(posts) { _, post ->
            Post(post)
        }
        item {
            if(loading) {
                CircularProgressIndicator()
            }
        }
    }
}