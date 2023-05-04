/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.navigation.component.post

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnResume
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.SearchOptions
import ru.herobrine1st.e621.api.await
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.ui.screen.post.logic.PostCommentsSource
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.e621.util.InstanceBase
import java.io.IOException

//private const val STATE_KEY = "POST_COMPONENT_STATE_KEY"
private const val TAG = "PostComponent"

class PostComponent(
    val openComments: Boolean,
    val query: SearchOptions,
    private val postId: Int,
    initialPost: Post?,
    componentContext: ComponentContext,
    private val navigator: StackNavigator<Config>,
    applicationContext: Context,
    exceptionReporter: ExceptionReporter,
    private val exoPlayer: ExoPlayer,
    val api: API
) : ComponentContext by componentContext {
    private val instance = instanceKeeper.getOrCreate {
        Instance(postId, api, exceptionReporter)
    }

    private val lifecycleScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // private var rememberedPositionMs = -1L
    var post by mutableStateOf(initialPost)
        private set

    var isLoadingPost by mutableStateOf(initialPost != null)
        private set

    // TODO move to another component (it should be overlay component)
    val commentsFlow get() = instance.commentsFlow

    init {
//        stateKeeper.register(STATE_KEY) {
//            State(contentPositionMs = rememberedPositionMs)
//        }
//        stateKeeper.consume<State>(STATE_KEY)?.let {
//            rememberedPositionMs = it.contentPositionMs
//        }

        lifecycle.doOnResume {
            // TODO preference to update on resume
            lifecycleScope.launch {
                val isDataSaverEnabled =
                    applicationContext.getPreferencesFlow { it.dataSaverModeEnabled }
                        .first()
                val currentPost = post
                val id = post?.id ?: postId
                if (currentPost == null // Nothing to show
                    || !isDataSaverEnabled
                ) {
                    isLoadingPost = true
                    try {
                        post = api.getPost(id).await().post
                        // Maybe reload ExoPlayer if old object contains invalid URL?
                        // exoPlayer.playbackState may help with that
                    } catch (e: IOException) {
                        Log.e(TAG, "Unable to get post $id", e)
                        exceptionReporter.handleNetworkException(e)
                    } catch (t: Throwable) {
                        Log.e(TAG, "Unable to get post $id", t)
                    }
                }
                isLoadingPost = false
                setMediaItem()
            }
            // Does not work, seekTo should be called after video is loaded
//            if (rememberedPositionMs > 0)
//                exoPlayer.seekTo(rememberedPositionMs)
        }
//        lifecycle.doOnPause {
//            rememberedPositionMs = exoPlayer.contentPosition
//            exoPlayer.clearMediaItems()
//        }
        lifecycle.doOnDestroy {
            lifecycleScope.cancel()
            exoPlayer.clearMediaItems()
        }
    }

    private fun setMediaItem() {
        assert(post != null) { "setMediaItem should be called only after post loading" }
        assert(exoPlayer.mediaItemCount == 0)
        val post = post!!
        if (post.file.type.isVideo) {
            exoPlayer.setMediaItem(MediaItem.fromUri(post.files.first { it.type.isVideo }.urls.first()))
            exoPlayer.prepare()
        }

    }

    fun handleWikiClick(tag: String) {
        navigator.pushIndexed {
            Config.Wiki(tag = tag, index = it)
        }
    }

    fun handleTagModification(tag: String, exclude: Boolean) {
        val searchOptions = query.toBuilder {
            if (exclude) {
                tags.remove(tag)
                if (!tags.contains("-$tag"))
                    tags.add("-$tag")
            } else
                if (!tags.contains(tag))
                    tags.add(tag)
        }
        navigator.pushIndexed { index -> Config.Search(searchOptions, index = index) }
    }

    class Instance(
        postId: Int,
        api: API,
        exceptionReporter: ExceptionReporter,
    ) : InstanceBase() {
        private val pager = Pager(
            PagingConfig(
                pageSize = BuildConfig.PAGER_PAGE_SIZE,
                initialLoadSize = BuildConfig.PAGER_PAGE_SIZE
            )
        ) {
            PostCommentsSource(api, exceptionReporter, postId)
        }

        val commentsFlow = pager.flow.cachedIn(lifecycleScope)
    }

//    @Parcelize
//    private data class State(
//        val contentPositionMs: Long
//    ) : Parcelable
}