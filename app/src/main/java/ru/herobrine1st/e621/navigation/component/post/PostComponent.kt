/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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
import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.doOnResume
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.common.VoteResult
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Pool
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.PostId
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.api.model.selectSample
import ru.herobrine1st.e621.api.search.PoolSearchOptions
import ru.herobrine1st.e621.api.search.PostsSearchOptions
import ru.herobrine1st.e621.api.search.SearchOptions
import ru.herobrine1st.e621.database.repository.vote.VoteRepository
import ru.herobrine1st.e621.module.CachedDataStore
import ru.herobrine1st.e621.module.DataStoreModule
import ru.herobrine1st.e621.module.IDownloadManager
import ru.herobrine1st.e621.module.PreferencesStore
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.navigation.component.VideoPlayerComponent
import ru.herobrine1st.e621.navigation.component.posts.handleFavouriteChange
import ru.herobrine1st.e621.navigation.component.posts.handleVote
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.e621.util.FavouritesCache
import ru.herobrine1st.e621.util.InstanceBase
import ru.herobrine1st.e621.util.isFavourite
import ru.herobrine1st.paging.api.PagingConfig
import ru.herobrine1st.paging.api.cachedIn
import ru.herobrine1st.paging.createPager

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
    private val downloadManager: IDownloadManager,
    private val voteRepository: VoteRepository,
    private val dataStoreModule: DataStoreModule
) : ComponentContext by componentContext {

    private val dataStore: PreferencesStore by dataStoreModule::dataStore

    private val instance = instanceKeeper.getOrCreate {
        Instance(postId, api, exceptionReporter)
    }

    private val lifecycleScope = LifecycleScope()


    // private var rememberedPositionMs = -1L
    private val state = MutableValue<PostState>(PostState.Loading)

    var currentFile by mutableStateOf(NormalizedFile.STUB)
        private set


    // TODO move to another component (it should be overlay component)
    val commentsFlow get() = instance.commentsFlow

    private lateinit var videoPlayerComponent: VideoPlayerComponent

    private var latestPlayedFile: NormalizedFile? = null

    //region Temporary: members for interface extraction

    val postState: Value<PostState> by ::state

    private val poolsLifecycle = LifecycleRegistry(Lifecycle.State.STARTED)

    val poolsComponent: PoolsComponent = PoolsComponentImpl(
        postState = postState,
        exceptionReporter = exceptionReporter,
        api = api,
        openPool = {
            poolsLifecycle.stop()
            openPool(it)
        },
        onActivateRequest = {
            poolsLifecycle.resume()
        },
        onDismissRequest = {
            poolsLifecycle.stop()
        },
        componentContext = childContext("PoolsComponentImpl", poolsLifecycle)
    )


    //endregion

    @CachedDataStore
    val preferences by dataStoreModule::cachedData

    @CachedDataStore
    val isAuthorized
        @Composable
        get() = preferences.collectAsState().value.auth != null

    init {
        check(initialPost == null || initialPost.id == postId)
        stateKeeper.register(POST_STATE_KEY, strategy = PostState.serializer()) { state.value }


        state.value = stateKeeper.consume(POST_STATE_KEY, strategy = PostState.serializer())
            ?: initialPost?.let { PostState.Ready(it) } ?: PostState.Loading

        if (state.value is PostState.Ready) useSampleAsDefault()

        lifecycle.doOnResume {
            // TODO behavior preference
            // 1. Do not refresh on resume/open
            // 2. Do refresh on open, do not refresh on resume
            // 3. Do both
            lifecycleScope.launch {
                if (
                    state.value !is PostState.Ready
                    || !dataStore.data.map { it.dataSaverModeEnabled }.first()
                )
                    refreshPostInternal()
                useSampleAsDefault()
            }

        }
    }

    private suspend fun refreshPostInternal() {
        (state.value as? PostState.Ready)?.let {
            state.value = it.copy(isUpdating = true)
        } ?: run {
            if (state.value is PostState.Error) state.value = PostState.Loading
        }
        api.getPost(postId).map {
            PostState.Ready(it.post)
        }.onSuccess {
            state.value = it
        }.onFailure {
            if (state.value !is PostState.Ready) state.value = PostState.Error
            exceptionReporter.handleRequestException(it)
        }
    }

    fun setFile(file: NormalizedFile) {
        currentFile = file
        if (file.type.isVideo) {
            getVideoPlayerComponent(file)
        }
    }

    private fun useSampleAsDefault() {
        val state = state.value
        require(state is PostState.Ready) { "setMediaItem should be called only after post loading" }
        if (currentFile !== NormalizedFile.STUB) return
        setFile(state.post.selectSample())
    }

    fun getVideoPlayerComponent(file: NormalizedFile): VideoPlayerComponent {
        require(file.type.isVideo) {
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
                mediaOkHttpClient = mediaOkHttpClientProvider.value,
                dataStoreModule = dataStoreModule
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
                favouritesCache, api, snackbarAdapter, (state.value as PostState.Ready).post
            )
        }
    }

    @Composable
    fun isFavouriteAsState(): State<FavouritesCache.FavouriteState> = remember {
        // Assume two things:
        // 1. This method is called after post is loaded
        // 2. post id will never change
        favouritesCache.flow.map { cache ->
            cache.isFavourite((state.value as PostState.Ready).post)
        }
    }.collectAsState(favouritesCache.isFavourite((state.value as PostState.Ready).post))

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
                id = (state.value as PostState.Ready).post.relationships.parentId!!,
                post = null,
                query = PostsSearchOptions(),
                index = index
            )
        }
    }

    fun openChildrenPostListing() {
        val post = (state.value as PostState.Ready).post
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

    private fun openPool(pool: Pool) {
        val searchOptions = PoolSearchOptions(pool)
        navigator.pushIndexed { index -> Config.PostListing(searchOptions, index = index) }
    }

    fun openToFullscreen() {
        val post = (state.value as PostState.Ready).post
        navigator.pushNew(
            Config.PostMedia(
                post = post,
                initialFile = currentFile
            )
        )
    }

    fun downloadFile() {
        val post = (state.value as? PostState.Ready) ?: return
        downloadManager.downloadFile(post.post.normalizedFile)
    }

    suspend fun vote(postId: PostId, @IntRange(from = -1, to = 1) vote: Int): VoteResult? {
        val response = handleVote(postId, vote, api, exceptionReporter)
        if (response != null) voteRepository.setVote(postId, response.ourScore)
        return response?.let { VoteResult(it.ourScore, it.total) }
    }

    suspend fun getVote(postId: PostId): Int? = voteRepository.getVote(postId)

    class Instance(
        postId: PostId,
        api: API,
        exceptionReporter: ExceptionReporter,
    ) : InstanceBase() {

        val commentsFlow = createPager(
            PagingConfig(
                pageSize = BuildConfig.PAGER_PAGE_SIZE,
                initialLoadSize = BuildConfig.PAGER_PAGE_SIZE,
                maxPagesInMemory = BuildConfig.PAGER_MAX_PAGES_IN_MEMORY
            ),
            initialKey = Int.MIN_VALUE,
            PostCommentsSource(api, exceptionReporter, postId)
        ).cachedIn(lifecycleScope)
    }
}

@Serializable
sealed interface PostState {

    @Serializable
    data object Loading : PostState

    @Serializable
    data object Error : PostState

    @Serializable
    data class Ready(val post: Post, val isUpdating: Boolean = false) : PostState
}