package ru.herobrine1st.e621.ui.screen.posts

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.LocalAPI
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.preference.PRIVACY_MODE
import ru.herobrine1st.e621.preference.getPreferenceFlow
import ru.herobrine1st.e621.ui.snackbar.LocalSnackbar

private const val TAG = "Post Screen"

@Composable
fun Post(
    initialPost: Post,
    @Suppress("UNUSED_PARAMETER") scrollToComments: Boolean // TODO
) {
    val api = LocalAPI.current
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val post by produceState(initialValue = initialPost) {
        if (!initialPost.isFavorited && context.getPreferenceFlow(PRIVACY_MODE, true).first()) {
            return@produceState
        }
        try {
            value = withContext(Dispatchers.IO) {
                api.fetchPostIfUpdated(initialPost)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to get post", t)
            snackbar.enqueueMessage(
                R.string.network_error,
                SnackbarDuration.Indefinite
            )
        }
    }

    LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
        item("media") {
            when {
                post.file.type.isVideo -> PostVideo(post.files.first { it.type.isVideo })
                post.file.type.isImage -> PostImage(
                    post = post,
                    aspectRatio = post.normalizedFile.aspectRatio,
                    openPost = null,
                    file = post.normalizedSample
                )
                else -> InvalidPost(text = stringResource(R.string.unsupported_post_type))
            }
        }
        item("todo") {
            Text("TODO")
        }
        // TODO comments
        // TODO i18n
        tags("Artist", post.tags.artist)
        tags("Copyright", post.tags.copyright)
        tags("Character", post.tags.character)
        tags("Species", post.tags.species)
        tags("General", post.tags.general)
        tags("Lore", post.tags.lore)
        tags("Meta", post.tags.meta)
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.tags(title: String, tags: List<String>) {
    if (tags.isEmpty()) return
    stickyHeader("$title tags") {
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
                title,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.weight(1f)
            ) // TODO i18n
        }
    }
    items(tags, key = { it }) {
        Tag(it)
    }
}

@Composable
fun Tag(tag: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(tag, modifier = Modifier.weight(1f))
        IconButton(
            onClick = {
                TODO()
            }
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add) // TODO i18n something more suitable
            )
        }
        IconButton(
            onClick = {
                TODO()
            }
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.remove) // TODO i18n something more suitable
            )
        }
        IconButton(
            onClick = {
                TODO()
            }
        ) {
            Icon(
                Icons.Default.Help,
                contentDescription = stringResource(R.string.remove) // TODO i18n something more suitable
            )
        }
    }
}