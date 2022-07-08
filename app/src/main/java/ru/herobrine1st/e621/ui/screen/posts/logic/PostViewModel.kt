package ru.herobrine1st.e621.ui.screen.posts.logic

import android.content.Context
import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.WikiPage
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.await
import ru.herobrine1st.e621.util.awaitResponse
import java.io.IOException

class PostViewModel @AssistedInject constructor(
    @ApplicationContext context: Context,
    val api: API,
    val snackbar: SnackbarAdapter,
    val exoPlayer: ExoPlayer,
    @Assisted initialPost: Post
) : ViewModel() {

    var post by mutableStateOf(initialPost)
        private set
    var isLoadingPost by mutableStateOf(true)
        private set

    var wikiState by mutableStateOf<WikiResult?>(null)
        private set
    private var wikiClickJob: Job? = null


    init {
        viewModelScope.launch {
            if (!initialPost.isFavorited && context.getPreferencesFlow { it.privacyModeEnabled }
                    .first()) {
                return@launch
            }
            try {
                post = api.getPost(initialPost.id).await().post
                isLoadingPost = true
                // Maybe reload ExoPlayer if old object contains invalid URL?
                // exoPlayer.playbackState may help with that
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
        exoPlayer.setMediaItem(MediaItem.fromUri(post.files.first { it.type.isVideo }.urls.first()))
        exoPlayer.prepare()
    }

    override fun onCleared() {
        exoPlayer.clearMediaItems()
    }

    fun handleWikiClick(tag: String) {
        if (wikiClickJob != null) throw IllegalStateException()
        wikiState = WikiResult.Loading(tag)
        wikiClickJob = viewModelScope.launch {
            val firstResponse = api.getWikiPageId(tag).awaitResponse()
            if (!firstResponse.raw().isRedirect) {
                wikiState = WikiResult.NotFound(tag)
                return@launch
            }
            val id = firstResponse.raw().header("Location")?.let {
                it.substring(it.lastIndexOf("/") + 1).toIntOrNull()
            }
            if (id == null) {
                Log.e(TAG, "Invalid redirection: Location header is not found or is not parsed")
                Log.e(TAG, firstResponse.raw().headers.joinToString("\n") {
                    it.first + ": " + it.second
                })
                wikiState = WikiResult.Failure(tag)
                return@launch
            }
            wikiState = WikiResult.Success(api.getWikiPage(id).await())
            wikiClickJob = null
        }

    }

    fun closeWikiPage() {
        wikiClickJob?.let {
            it.cancel()
            wikiClickJob = null
        }
        wikiState = null
    }


    // Assisted inject stuff

    @AssistedFactory
    interface Factory {
        fun create(initialPost: Post): PostViewModel
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface FactoryProvider {
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("providePostViewModelFactory")
        fun provideFactory(): Factory
    }

    companion object {
        const val TAG = "PostViewModel"

        @Suppress("UNCHECKED_CAST")
        fun provideFactory(
            assistedFactory: Factory,
            initialPost: Post
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(initialPost) as T
            }
        }
    }
}

sealed class WikiResult(val title: String) {
    class Loading(tag: String) : WikiResult(tag)
    class Success(val result: WikiPage) : WikiResult(result.title)
    class Failure(tag: String) : WikiResult(tag)
    class NotFound(tag: String) : WikiResult(tag)
}