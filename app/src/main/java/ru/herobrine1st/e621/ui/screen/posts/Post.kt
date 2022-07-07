package ru.herobrine1st.e621.ui.screen.posts

import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.flow.first
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.module.LocalAPI
import ru.herobrine1st.e621.module.LocalExoPlayer
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.service.PlayerService
import ru.herobrine1st.e621.ui.snackbar.LocalSnackbar
import ru.herobrine1st.e621.util.await
import ru.herobrine1st.e621.util.debug
import java.io.IOException

private const val TAG = "Post Screen"

@Composable
fun Post(
    initialPost: Post,
    @Suppress("UNUSED_PARAMETER") scrollToComments: Boolean, // TODO
    onExit: () -> Unit,
) {
    val api = LocalAPI.current
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current

    val post by produceState(initialValue = initialPost) {
        if (!initialPost.isFavorited && context.getPreferencesFlow { it.privacyModeEnabled }
                .first()) {
            return@produceState
        }
        try {
            value = api.getPost(initialPost.id).await().post
        } catch (e: IOException) {
            Log.e(TAG, "Unable to get post ${initialPost.id}", e)
            snackbar.enqueueMessage(
                R.string.network_error,
                SnackbarDuration.Indefinite
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to get post ${initialPost.id}", t)
        }
    }

    ExoPlayerHandler(post, onExit)

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
    stickyHeader("$title tags") { // TODO i18n
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
                debug {
                    TODO()
                }

            }
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add) // TODO i18n something more suitable
            )
        }
        IconButton(
            onClick = {
                debug {
                    TODO()
                }
            }
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = stringResource(R.string.remove) // TODO i18n something more suitable
            )
        }
        IconButton(
            onClick = {
                debug {
                    TODO()
                }
            }
        ) {
            Icon(
                Icons.Default.Help,
                contentDescription = stringResource(R.string.remove) // TODO i18n something more suitable
            )
        }
    }
}


// Set media item only on first composition in this scope (likely it is a navigation graph)
// Clear media item on exit
// Like DisposableEffect, but in scope of a graph
// Cannot use RememberObserver because onForgotten is triggered on decomposition even if rememberSaveable is used
@Composable
fun ExoPlayerHandler(post: Post, onExit: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = LocalExoPlayer.current
    var mediaItemIsSet by rememberSaveable { mutableStateOf(false) }
    if(post.file.type.isVideo && !mediaItemIsSet) LaunchedEffect(Unit) {
        exoPlayer.setMediaItem(MediaItem.fromUri(post.files.first { it.type.isVideo }.urls.first()))
        exoPlayer.prepare()
        Intent(context, PlayerService::class.java).let {
            context.startService(it)
        }
        mediaItemIsSet = true
    }

    BackHandler {
        Intent(context, PlayerService::class.java).let {
            context.stopService(it)
        }
        exoPlayer.clearMediaItems()
        onExit()
    }
}