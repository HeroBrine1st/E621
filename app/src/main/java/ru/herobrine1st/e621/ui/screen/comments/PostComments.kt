package ru.herobrine1st.e621.ui.screen.comments

import android.app.Activity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import dagger.hilt.android.EntryPointAccessors
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.endOfPagePlaceholder
import ru.herobrine1st.e621.ui.screen.comments.component.PostComment

@Composable
fun PostComments(
    postId: Int,
    viewModel: PostCommentsViewModel = viewModel(
        factory = PostCommentsViewModel.provideFactory(
            EntryPointAccessors.fromActivity(
                LocalContext.current as Activity,
                PostCommentsViewModel.FactoryProvider::class.java
            ).provideFactory(), postId
        )
    )
) {
    val comments = viewModel.commentsFlow.collectAsLazyPagingItems()
    val lazyListState = rememberLazyListState()

    if (comments.loadState.refresh !is LoadState.NotLoading) {  // Do not reset lazyListState
        Base {
            Spacer(modifier = Modifier.height(4.dp))
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        state = lazyListState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        endOfPagePlaceholder(comments.loadState.prepend)
        item { Spacer(Modifier.height(4.dp)) }
        items(comments, key = { it.first.id }) {
            if (it == null) return@items
            PostComment(it.first, it.second)
            Spacer(Modifier.height(4.dp))
        }
        endOfPagePlaceholder(comments.loadState.append)
    }
}