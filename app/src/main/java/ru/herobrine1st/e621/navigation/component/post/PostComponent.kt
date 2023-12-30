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
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnResume
import com.arkivanov.essenty.parcelable.Parcelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okhttp3.OkHttpClient
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.api.SearchOptions
import ru.herobrine1st.e621.api.await
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Order
import ru.herobrine1st.e621.api.model.PoolId
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.api.model.selectSample
import ru.herobrine1st.e621.navigation.component.VideoPlayerComponent
import ru.herobrine1st.e621.navigation.component.posts.handleFavouriteChange
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.ui.screen.post.logic.PostCommentsSource
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.e621.util.FavouritesCache
import ru.herobrine1st.e621.util.InstanceBase
import ru.herobrine1st.e621.util.isFavourite
import java.io.IOException

private const val POST_STATE_KEY = "POST_STATE_KEY"
private const val TAG = "PostComponent"

class PostComponent(
    val openComments: Boolean,
    private val query: SearchOptions,
    private val postId: Int,
    initialPost: Post?,
    componentContext: ComponentContext,
    private val navigator: StackNavigator<Config>,
    private val applicationContext: Context,
    exceptionReporter: ExceptionReporter,
    private val api: API,
    private val favouritesCache: FavouritesCache,
    private val snackbarAdapter: SnackbarAdapter,
    private val mediaOkHttpClientProvider: Lazy<OkHttpClient>,
) : ComponentContext by componentContext {
    private val instance = instanceKeeper.getOrCreate {
        Instance(postId, api, exceptionReporter)
    }

    private val lifecycleScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val slotNavigation = SlotNavigation<PoolsDialogConfig>()

    val dialog: Value<ChildSlot<PoolsDialogConfig, PoolsDialogComponent>> = childSlot(
        source = slotNavigation,
        handleBackButton = true,
        childFactory = { _, componentContext ->
            return@childSlot PoolsDialogComponent(
                componentContext = componentContext,
                api = api,
                pools = post!!.pools,
                openPool = ::openPool,
                close = ::closePoolDialog
            )
        }
    )

    // private var rememberedPositionMs = -1L
    var post by mutableStateOf(initialPost)
        private set

    var isLoadingPost by mutableStateOf(initialPost != null)
        private set

    @Composable
    fun isFavouriteAsState(): State<FavouritesCache.FavouriteState> = remember {
        // Assume two things:
        // 1. This method is called after post is loaded
        // 2. [post] (particularly, its id) will never change
        favouritesCache.flow.map { cache ->
            cache.isFavourite(post!!)
        }
    }.collectAsState(favouritesCache.isFavourite(post!!))

    fun handleFavouriteChange() {
        // Assume post is loaded here as well
        lifecycleScope.launch {
            handleFavouriteChange(
                favouritesCache,
                api,
                snackbarAdapter,
                post!!
            )
        }
    }

    // TODO move to another component (it should be overlay component)
    val commentsFlow get() = instance.commentsFlow

    private lateinit var videoPlayerComponent: VideoPlayerComponent
    private var latestPlayedFile: NormalizedFile? = null

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

    init {
        stateKeeper.register(POST_STATE_KEY, strategy = Post.serializer()) {
            post
        }
        lifecycle.doOnResume {
            // TODO behavior preference
            // 1. Do not refresh on resume/open
            // 2. Do refresh on open, do not refresh on resume
            // 3. Do both
            stateKeeper.consume(POST_STATE_KEY, strategy = Post.serializer())?.let {
                post = it
                // Okay, there might be flicker
                // idk what to do, probably should cache DataStore using StateFlow
                // FIXME possible flicker
                isLoadingPost = false
                return@doOnResume
            }
            lifecycleScope.launch {
                val id = post?.id ?: postId
                if (post == null // Nothing to show
                    || applicationContext.getPreferencesFlow { !it.dataSaverModeEnabled }.first()
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
        }
        lifecycle.doOnDestroy {
            lifecycleScope.cancel()
        }
    }

    private fun setMediaItem() {
        assert(post != null) { "setMediaItem should be called only after post loading" }
        val post = post!!
        if (post.file.type.isVideo) {
            getVideoPlayerComponent(post.selectSample())
        }

    }

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
                id = post!!.relationships.parentId!!,
                post = null,
                query = PostsSearchOptions(),
                index = index
            )
        }
    }

    fun openChildrenPostListing() {
        val post = post!!
        post.relationships.children.singleOrNull()?.let { id ->
            navigator.pushIndexed {
                Config.Post(
                    id = id,
                    post = null,
                    query = PostsSearchOptions(),
                    index = it
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
                ),
                index = index
            )
        }
    }

    fun openPools() {
        val post = post!!
        post.pools.singleOrNull()?.let {
            openPool(it)
            return
        }
        slotNavigation.navigate { PoolsDialogConfig }
    }

    private fun openPool(id: PoolId) {
        closePoolDialog()
        // TODO find a way to get posts in right order, downloading them in the same API call
        //      because pool may be ordered differently to order:id
        // It is known that there's a way to get order of posts, but not ordered posts themselves.
        // If it isn't possible, user feedback is required *before* any workarounds are implemented.
        // Particularly, there needs to find pool with that order anomaly to test hypothetical workarounds.
        val searchOptions = PostsSearchOptions(
            order = Order.NEWEST_TO_OLDEST,
            orderAscending = true, // thus oldest to newest
            poolId = id
        )
        Log.d(TAG, "Making search options for pool $id: $searchOptions")
        navigator.pushIndexed { index -> Config.PostListing(searchOptions, index = index) }
    }

    private fun closePoolDialog() {
        slotNavigation.navigate { null }
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
}

@Parcelize
object PoolsDialogConfig : Parcelable