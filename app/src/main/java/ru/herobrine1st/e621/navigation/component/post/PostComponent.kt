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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.navigate
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnResume
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.PoolSearchOptions
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.api.SearchOptions
import ru.herobrine1st.e621.api.model.FileType
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Pool
import ru.herobrine1st.e621.api.model.PoolId
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.PostId
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.api.model.selectSample
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.navigation.component.VideoPlayerComponent
import ru.herobrine1st.e621.navigation.component.posts.handleFavouriteChange
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.e621.util.FavouritesCache
import ru.herobrine1st.e621.util.InstanceBase
import ru.herobrine1st.e621.util.isFavourite

private const val POST_STATE_KEY = "POST_STATE_KEY"

class PostComponent(
    val openComments: Boolean,
    private val query: SearchOptions,
    private val postId: PostId,
    initialPost: Post?,
    componentContext: ComponentContext,
    private val navigator: StackNavigator<Config>,
    private val applicationContext: Context,
    private val exceptionReporter: ExceptionReporter,
    private val api: API,
    private val favouritesCache: FavouritesCache,
    private val snackbarAdapter: SnackbarAdapter,
    private val mediaOkHttpClientProvider: Lazy<OkHttpClient>,
) : ComponentContext by componentContext {
    private val instance = instanceKeeper.getOrCreate {
        Instance(postId, api, exceptionReporter)
    }

    private val lifecycleScope = LifecycleScope()

    private val slotNavigation = SlotNavigation<PoolsDialogConfig>()

    val dialog: Value<ChildSlot<PoolsDialogConfig, PoolsDialogComponent>> =
        childSlot(source = slotNavigation,
            serializer = PoolsDialogConfig.serializer(),
            handleBackButton = true,
            childFactory = { _, componentContext ->
                return@childSlot PoolsDialogComponent(
                    componentContext = componentContext,
                    api = api,
                    pools = (state as PostState.Ready).post.pools,
                    openPool = {
                        openPool(it.id, pool = it)
                    },
                    close = ::closePoolDialog
                )
            })

    // private var rememberedPositionMs = -1L
    var state by mutableStateOf<PostState>(PostState.Loading)
        private set
    var currentFile by mutableStateOf(
        NormalizedFile(
            "stub",
            0,
            0,
            FileType.UNDEFINED,
            0,
            emptyList()
        )
    )
        private set


    // TODO move to another component (it should be overlay component)
    val commentsFlow get() = instance.commentsFlow

    private lateinit var videoPlayerComponent: VideoPlayerComponent

    private var latestPlayedFile: NormalizedFile? = null

    init {
        assert(initialPost == null || initialPost.id == postId)
        stateKeeper.register(POST_STATE_KEY, strategy = PostState.serializer()) { state }


        state = stateKeeper.consume(POST_STATE_KEY, strategy = PostState.serializer())
            ?: initialPost?.let { PostState.Ready(it) } ?: PostState.Loading

        if (state is PostState.Ready) setMediaItem()

        lifecycle.doOnResume {
            // TODO behavior preference
            // 1. Do not refresh on resume/open
            // 2. Do refresh on open, do not refresh on resume
            // 3. Do both
            lifecycleScope.launch {
                if (
                    state !is PostState.Ready
                    || !applicationContext.getPreferencesFlow { it.dataSaverModeEnabled }.first()
                )
                    refreshPostInternal()
                setMediaItem()
            }

        }
    }

    private suspend fun refreshPostInternal() {
        (state as? PostState.Ready)?.let {
            state = it.copy(isUpdating = true)
        } ?: run {
            if (state is PostState.Error) state = PostState.Loading
        }
        api.getPost(postId).map {
            PostState.Ready(it.post)
        }.onSuccess {
            state = it
        }.onFailure {
            if (state !is PostState.Ready) state = PostState.Error
            exceptionReporter.handleRequestException(it)
        }
    }

    private fun setMediaItem() {
        val state = state
        assert(state is PostState.Ready) { "setMediaItem should be called only after post loading" }
        state as PostState.Ready
        currentFile = state.post.selectSample()
        if (currentFile.type.isVideo) {
            getVideoPlayerComponent(currentFile)
        }
    }

    fun getVideoPlayerComponent(file: NormalizedFile): VideoPlayerComponent {
        assert(file.type.isVideo) {
            "File is not video"
        }
        // This function can be called in a result of scrolling down and back
        // also recomposition can occur when navigating back, i.e. components are destroyed
        if (latestPlayedFile == file) {
            return videoPlayerComponent
        }
        latestPlayedFile = file
        val url = file.urls.first()
        if (::videoPlayerComponent.isInitialized) {
            videoPlayerComponent.setUrl(url)
        } else {
            videoPlayerComponent = VideoPlayerComponent(
                url = url,
                applicationContext = applicationContext,
                componentContext = childContext("VIDEO_COMPONENT"),
                mediaOkHttpClient = mediaOkHttpClientProvider.value
            )
        }
        return videoPlayerComponent
    }

    fun refreshPost() = lifecycleScope.launch {
        refreshPostInternal()
    }

    fun handleFavouriteChange() {
        // Assume post is loaded here as well
        lifecycleScope.launch {
            handleFavouriteChange(
                favouritesCache, api, snackbarAdapter, (state as PostState.Ready).post
            )
        }
    }

    @Composable
    fun isFavouriteAsState(): State<FavouritesCache.FavouriteState> = remember {
        // Assume two things:
        // 1. This method is called after post is loaded
        // 2. post id will never change
        favouritesCache.flow.map { cache ->
            cache.isFavourite((state as PostState.Ready).post)
        }
    }.collectAsState(favouritesCache.isFavourite((state as PostState.Ready).post))

    fun handleWikiClick(tag: Tag) {
        navigator.pushIndexed {
            Config.Wiki(tag = tag, index = it)
        }
    }

    fun handleTagModification(tag: Tag, exclude: Boolean) {
        val searchOptions = query.toBuilder {
            if (exclude) {
                allOf.remove(tag)
                noneOf.add(tag)
            } else {
                allOf.add(tag)
                noneOf.remove(tag)
            }
        }
        navigator.pushIndexed { index -> Config.Search(searchOptions, index = index) }
    }

    fun openParentPost() {
        navigator.pushIndexed { index ->
            Config.Post(
                id = (state as PostState.Ready).post.relationships.parentId!!,
                post = null,
                query = PostsSearchOptions(),
                index = index
            )
        }
    }

    fun openChildrenPostListing() {
        val post = (state as PostState.Ready).post
        post.relationships.children.singleOrNull()?.let { id ->
            navigator.pushIndexed {
                Config.Post(
                    id = id, post = null, query = PostsSearchOptions(), index = it
                )
            }
            return
        }
        navigator.pushIndexed { index ->
            Config.PostListing(
                search = PostsSearchOptions(
                    // TODO get safe mode synchronously
                    // (post listing has filter for safe mode)
                    parent = post.id
                ), index = index
            )
        }
    }

    fun openPools() {
        val post = (state as PostState.Ready).post
        post.pools.singleOrNull()?.let {
            openPool(it)
            return
        }
        slotNavigation.navigate { PoolsDialogConfig }
    }

    private fun openPool(id: PoolId, pool: Pool? = null) {
        closePoolDialog()
        val searchOptions = PoolSearchOptions(poolId = id, postIds = pool?.posts)
        navigator.pushIndexed { index -> Config.PostListing(searchOptions, index = index) }
    }

    private fun closePoolDialog() {
        slotNavigation.navigate { null }
    }

    class Instance(
        postId: PostId,
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
}

@Serializable
object PoolsDialogConfig

@Serializable
sealed interface PostState {

    @Serializable
    data object Loading : PostState

    @Serializable
    data object Error : PostState

    @Serializable
    data class Ready(val post: Post, val isUpdating: Boolean = false) : PostState
}