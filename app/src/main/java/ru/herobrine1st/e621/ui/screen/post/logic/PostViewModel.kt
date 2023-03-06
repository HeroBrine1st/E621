/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.herobrine1st.e621.ui.screen.post.logic

import android.content.Context
import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.fasterxml.jackson.core.JacksonException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.*
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.WikiPage
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.JacksonExceptionHandler
import java.io.IOException

class PostViewModel @AssistedInject constructor(
    @ApplicationContext context: Context,
    val api: API,
    val snackbar: SnackbarAdapter,
    private val exoPlayer: ExoPlayer,
    private val jacksonExceptionHandler: JacksonExceptionHandler,
    @Assisted postId: Int,
    @Assisted initialPost: Post?
) : ViewModel() {

    var post by mutableStateOf(initialPost)
        private set
    var isLoadingPost by mutableStateOf(true)
        private set

    var wikiState by mutableStateOf<WikiResult?>(null)
        private set
    private var wikiClickJob: Job? = null

    private var mediaItemIsSet = false

    private val pager = Pager(
        PagingConfig(
            pageSize = BuildConfig.PAGER_PAGE_SIZE,
            initialLoadSize = BuildConfig.PAGER_PAGE_SIZE
        )
    ) {
        PostCommentsSource(api, snackbar, jacksonExceptionHandler, postId)
    }

    val commentsFlow = pager.flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            val isPrivacyModeEnabled = context.getPreferencesFlow { it.privacyModeEnabled }
                .first()
            val id = initialPost?.id ?: postId
            if (initialPost?.isFavorited != false || !isPrivacyModeEnabled) {
                try {
                    post = withContext(Dispatchers.IO) {
                        api.getPost(id).await().post
                    }
                    isLoadingPost = false
                    setMediaItem()
                    // Maybe reload ExoPlayer if old object contains invalid URL?
                    // exoPlayer.playbackState may help with that
                } catch (e: JacksonException) {
                    jacksonExceptionHandler.handleDeserializationError(e)
                } catch (e: IOException) {
                    Log.e(TAG, "Unable to get post $id", e)
                    snackbar.enqueueMessage(
                        R.string.network_error,
                        SnackbarDuration.Indefinite
                    )
                } catch (t: Throwable) {
                    Log.e(TAG, "Unable to get post $id", t)
                }
            }
        }
        setMediaItem()
    }

    override fun onCleared() {
        exoPlayer.clearMediaItems()
    }

    private fun setMediaItem() {
        val post = post ?: return
        if(mediaItemIsSet) return
        if (post.file.type.isVideo) {
            exoPlayer.setMediaItem(MediaItem.fromUri(post.files.first { it.type.isVideo }.urls.first()))
            exoPlayer.prepare()
        }
        mediaItemIsSet = true
    }

    fun handleWikiClick(tag: String) {
        if (wikiClickJob != null) throw IllegalStateException()
        wikiState = WikiResult.Loading(tag)
        wikiClickJob = viewModelScope.launch {
            wikiState = try {
                WikiResult.Success(api.getWikiPage(tag))
            } catch (e: NotFoundException) {
                WikiResult.NotFound(tag)
            } catch (e: ApiException) {
                WikiResult.Failure(tag)
            } catch (t: Throwable) {
                Log.e(TAG, "Unknown error occurred while downloading wiki page", t)
                WikiResult.Failure(tag)
            } finally {
                wikiClickJob = null
            }
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
        fun create(
            postId: Int,
            initialPost: Post?
        ): PostViewModel
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
            postId: Int,
            initialPost: Post?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(postId, initialPost) as T
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