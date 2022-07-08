package ru.herobrine1st.e621.ui.screen.posts

import android.app.Activity
import android.text.format.DateUtils
import android.text.format.DateUtils.SECOND_IN_MILLIS
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import dagger.hilt.android.EntryPointAccessors
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.dialog.ContentDialog
import ru.herobrine1st.e621.ui.screen.posts.component.PostImage
import ru.herobrine1st.e621.ui.screen.posts.logic.PostViewModel
import ru.herobrine1st.e621.ui.screen.posts.logic.WikiResult
import ru.herobrine1st.e621.util.PostsSearchOptions
import ru.herobrine1st.e621.util.SearchOptions
import java.util.*

private const val TAG = "Post Screen"

@Composable
fun Post(
    initialPost: Post,
    @Suppress("UNUSED_PARAMETER") scrollToComments: Boolean, // TODO
    searchOptions: SearchOptions,
    onModificationClick: (PostsSearchOptions) -> Unit,
    viewModel: PostViewModel = viewModel(
        factory = PostViewModel.provideFactory(
            EntryPointAccessors.fromActivity(
                LocalContext.current as Activity,
                PostViewModel.FactoryProvider::class.java
            ).provideFactory(), initialPost
        )
    )
) {
    val post = viewModel.post
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val wikiState = viewModel.wikiState
    if (wikiState != null) {
        ContentDialog(
            title = wikiState.title.replaceFirstChar { // Capitalize
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
            onDismissRequest = { viewModel.closeWikiPage() }) {
            LazyColumn(
                modifier = Modifier.height(screenHeight * 0.4f)
            ) {
                when (wikiState) {
                    is WikiResult.Loading -> items(50) {
                        Text(
                            "", modifier = Modifier
                                .fillMaxWidth()
                                .placeholder(true, highlight = PlaceholderHighlight.shimmer())
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    is WikiResult.Failure -> item {
                        Text(stringResource(R.string.wiki_load_failed))
                    }
                    is WikiResult.NotFound -> item {
                        Text(stringResource(R.string.not_found))
                    }
                    is WikiResult.Success -> item {
                        Text(wikiState.result.body)
                    }
                }
            }
        }
    }

    LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
        item("media") {
            when {
                post.file.type.isVideo -> PostVideo(post.files.first { it.type.isVideo })
                post.file.type.isImage -> PostImage(
                    post = post,
                    openPost = null,
                    file = post.normalizedSample
                )
                else -> InvalidPost(
                    text = stringResource(
                        R.string.unsupported_post_type,
                        post.file.type.extension
                    )
                )
            }
        }
        item("todo") {
            Text("TODO")
        }
        // TODO comments
        // TODO i18n
        item("uploaded") {
            Text(
                stringResource(
                    R.string.uploaded_relative_date,
                    DateUtils.getRelativeTimeSpanString(
                        post.createdAt.toEpochSecond() * 1000,
                        System.currentTimeMillis(),
                        SECOND_IN_MILLIS
                    )
                ),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }
        tags(post, searchOptions, onModificationClick, onWikiClick = {
            viewModel.handleWikiClick(it)
        })
    }
}

fun LazyListScope.tags(
    post: Post,
    searchOptions: SearchOptions,
    onModificationClick: (PostsSearchOptions) -> Unit,
    onWikiClick: (String) -> Unit
) {
    tags(R.string.artist_tags, post.tags.artist, searchOptions, onModificationClick, onWikiClick)
    tags(R.string.copyright_tags, post.tags.copyright, searchOptions, onModificationClick, onWikiClick)
    tags(R.string.character_tags, post.tags.character, searchOptions, onModificationClick, onWikiClick)
    tags(R.string.species_tags, post.tags.species, searchOptions, onModificationClick, onWikiClick)
    tags(R.string.general_tags, post.tags.general, searchOptions, onModificationClick, onWikiClick)
    tags(R.string.lore_tags, post.tags.lore, searchOptions, onModificationClick, onWikiClick)
    tags(R.string.meta_tags, post.tags.meta, searchOptions, onModificationClick, onWikiClick)
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.tags(
    @StringRes titleId: Int,
    tags: List<String>,
    searchOptions: SearchOptions,
    onModificationClick: (PostsSearchOptions) -> Unit,
    onWikiClick: (String) -> Unit
) {
    if (tags.isEmpty()) return
    stickyHeader("$titleId tags") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 8.dp)
                .height(ButtonDefaults.MinHeight)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colors.background,
                            MaterialTheme.colors.background.copy(alpha = 0f)
                        )
                    )
                )
        ) {
            Text(
                stringResource(titleId),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.weight(1f)
            ) // TODO i18n
        }
    }
    items(tags, key = { "$it tag" }) {
        Tag(it, searchOptions, onModificationClick, onWikiClick)
    }
}

@Composable
fun Tag(
    tag: String,
    searchOptions: SearchOptions,
    onModificationClick: (PostsSearchOptions) -> Unit,
    onWikiClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(tag, modifier = Modifier.weight(1f))
        IconButton( // Add
            onClick = {
                onModificationClick(searchOptions.toBuilder { tags += tag })
            }
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add_tag_to_search)
            )
        }
        IconButton(
            onClick = {
                onModificationClick(searchOptions.toBuilder { tags += "-$tag" })
            }
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.exclude_tag_from_search)
            )
        }
        IconButton(
            onClick = {
                onWikiClick(tag)
            }
        ) {
            Icon(
                Icons.Default.Help,
                contentDescription = stringResource(R.string.tag_view_wiki)
            )
        }
    }
}