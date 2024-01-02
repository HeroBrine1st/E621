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

package ru.herobrine1st.e621.ui.screen.post

import android.text.format.DateUtils
import android.text.format.DateUtils.SECOND_IN_MILLIS
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.api.model.selectSample
import ru.herobrine1st.e621.navigation.component.post.PoolsDialogComponent
import ru.herobrine1st.e621.navigation.component.post.PostComponent
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.component.CollapsibleColumn
import ru.herobrine1st.e621.ui.component.MAX_SCALE_DEFAULT
import ru.herobrine1st.e621.ui.component.RenderBB
import ru.herobrine1st.e621.ui.component.endOfPagePlaceholder
import ru.herobrine1st.e621.ui.component.post.PostActionRow
import ru.herobrine1st.e621.ui.component.post.PostMediaContainer
import ru.herobrine1st.e621.ui.component.rememberZoomableState
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState
import ru.herobrine1st.e621.ui.component.zoomable
import ru.herobrine1st.e621.ui.screen.post.component.PoolsDialog
import ru.herobrine1st.e621.ui.screen.post.component.PostComment
import ru.herobrine1st.e621.ui.screen.post.data.CommentData
import ru.herobrine1st.e621.util.text

private const val DESCRIPTION_COLLAPSED_HEIGHT_FRACTION = 0.4f
private const val TAG = "Post Screen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Post(
    screenSharedState: ScreenSharedState,
    component: PostComponent,
    isAuthorized: Boolean, // TODO move to component
) {
    val post = component.post
    val preferences = LocalPreferences.current

    val coroutineScope = rememberCoroutineScope()

    var loadComments by remember {
        mutableStateOf(
            preferences.hasAuth() // Assuming there can't be invalid credentials in preferences
                    && (!preferences.dataSaverModeEnabled // Do not make excessive API calls on user preference
                    || component.openComments)
        )
    }

    if (post == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    var fullscreenState by remember { mutableStateOf(FullscreenState.CLOSED) }

    var imagePositionInRoot by remember { mutableStateOf(Offset.Unspecified) }

    val media = remember {
        movableContentOf { file: NormalizedFile, post: Post, modifier: Modifier, matchHeightConstrainsFirst: Boolean ->
            PostMediaContainer(
                file = file,
                contentDescription = remember(post.id) { post.tags.all.joinToString(" ") },
                modifier = modifier,
                getVideoPlayerComponent = {
                    component.getVideoPlayerComponent(file)
                },
                matchHeightConstraintsFirst = matchHeightConstrainsFirst
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()


    // STOPSHIP: on second frame, somehow it sets to PartiallyExpanded, avoiding "if" block
    // and it is instant, like it always was an initial value
    val bottomSheetState = rememberStandardBottomSheetState(
        // `&& loadComments` is a fix for unauthenticated usage case
        // Think of it as there is no point to opening comment if they're not loading
        // (yes we can && preferences.hasAuth(), but let's go with single source of truth, ok?
        // Auth logic may and will change sometime. Also && loadComments has less overhead - it is anyway already computed)
        initialValue = if (component.openComments && loadComments) SheetValue.PartiallyExpanded
        else SheetValue.Hidden,
        skipHiddenState = false
    )
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState,
        snackbarHostState = screenSharedState.snackbarHostState
    )

    val dialog by component.dialog.subscribeAsState()

    when (val dialogComponent = dialog.child?.instance) {
        is PoolsDialogComponent -> {
            PoolsDialog(component = dialogComponent)
        }

        null -> {}
    }

    BoxWithConstraints {
        BottomSheetScaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.post))
                    },
                    actions = {
                        ActionBarMenu(
                            onNavigateToSettings = screenSharedState.goToSettings,
                            onOpenBlacklistDialog = screenSharedState.openBlacklistDialog
                        )
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            sheetContent = {
                CommentsBottomSheetContent(
                    commentsFlow = component.commentsFlow,
                    post = post,
                    loadComments = loadComments
                )
            },
            sheetPeekHeight = maxHeight * 0.5f,
            scaffoldState = bottomSheetScaffoldState,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { _ ->
            if (preferences.safeModeEnabled && post.rating.isNotSafe) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Explicit, contentDescription = null)
                    Text(stringResource(R.string.safe_mode_blocks_post))
                    Spacer(Modifier.weight(1f))
                }
                return@BottomSheetScaffold
            }

            // PaddingValues purposely unused because it clutters screen if applied via modifier
            // and lags if applied via contentPadding
            // while has no top padding, so useless
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                userScrollEnabled = fullscreenState == FullscreenState.CLOSED
            ) {
                item("media") {
                    val sample = post.selectSample()

                    // "Open to fullscreen" behavior
                    // Currently no animation
                    Box(
                        Modifier
                            // This box will be empty if image is gone to fullscreen
                            .aspectRatio(sample.aspectRatio)
                            .fillMaxWidth()
                            .clickable(
                                enabled = fullscreenState == FullscreenState.CLOSED
                                        && !post.file.type.isVideo,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                fullscreenState = FullscreenState.OPEN
                            }
                            .onGloballyPositioned {
                                imagePositionInRoot = it.positionInRoot()
                            }
                    ) {
                        if (fullscreenState == FullscreenState.CLOSED)
                            media(sample, post, Modifier.fillMaxWidth(), false)
                    }
                }
                item("actionbar") {
                    val favouriteState by component.isFavouriteAsState()
                    PostActionRow(
                        post, favouriteState, isAuthorized,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth(),
                        onFavouriteChange = component::handleFavouriteChange
                    ) {
                        coroutineScope.launch {
                            loadComments = true
                            bottomSheetState.partialExpand()
                        }
                    }
                    HorizontalDivider()
                }
                // TODO visually connect description to image and add elevation only at bottom
                // (it should look great according to my imagination)
                // P.s. it is not possible with vanilla compose in compose stage; should draw manually
                item("description") {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()

                    ) {
                        Text(
                            stringResource(R.string.description),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CollapsibleColumn(
                            collapsedHeight = this@BoxWithConstraints.maxHeight * DESCRIPTION_COLLAPSED_HEIGHT_FRACTION,
                            button = { expanded, onClick ->
                                TextButton(
                                    onClick = onClick,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // TODO expose animation state from CollapsibleColumn and use it here
                                    val rotation: Float by animateFloatAsState(
                                        if (expanded) 180f else 360f,
                                        label = "Expand-collapse button icon rotation"
                                    )
                                    Icon(
                                        Icons.Default.ExpandMore, null, modifier = Modifier
                                            .padding(start = 4.dp, end = 12.dp)
                                            .rotate(rotation)
                                    )
                                    Crossfade(
                                        expanded,
                                        label = "Expand-collapse button text crossfade"
                                    ) { state ->
                                        Text(
                                            stringResource(if (!state) R.string.expand else R.string.collapse),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        ) {
                            if (post.description.isNotBlank()) SelectionContainer {
                                RenderBB(post.description)
                            } else {
                                Text(stringResource(R.string.empty_description))
                            }
                        }
                    }
                    HorizontalDivider()
                }
                item("comments") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = post.commentCount != 0 && preferences.hasAuth()) {
                                coroutineScope.launch {
                                    loadComments = true
                                    bottomSheetState.partialExpand()
                                }
                            }
                            .padding(horizontal = BASE_PADDING_HORIZONTAL)
                    ) {
                        Text(
                            stringResource(R.string.comments),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.width(4.dp))

                        val commentState = when {
                            post.commentCount == 0 -> {
                                CommentsLoadingState.Empty
                            }


                            loadComments -> {
                                // Cannot hoist comments: there's no disable for automatic download
                                val comments = component.commentsFlow.collectAsLazyPagingItems()
                                when (comments.loadState.refresh) {
                                    is LoadState.Loading -> {
                                        CommentsLoadingState.Showable.Loading
                                    }

                                    is LoadState.NotLoading -> {
                                        if (comments.itemSnapshotList.isEmpty()) CommentsLoadingState.Empty
                                        else comments.peek(0)
                                            ?.let { CommentsLoadingState.Showable.Success(it) }
                                            ?: run {
                                                Log.w(
                                                    TAG,
                                                    "itemSnapshotList is not empty but first item is not a show-able comment"
                                                )
                                                // It is not possible, but !! on UI thread is bad imho
                                                // so fall back gently
                                                CommentsLoadingState.Failed
                                            }
                                    }

                                    is LoadState.Error -> CommentsLoadingState.Failed
                                }
                            }

                            !preferences.hasAuth() -> {
                                CommentsLoadingState.Forbidden
                            }

                            else -> {
                                CommentsLoadingState.NotLoading
                            }
                        }
                        val transition = updateTransition(
                            targetState = commentState,
                            label = "Comment Animation"
                        )
                        transition.AnimatedContent(contentKey = { it.index }, transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                    fadeOut(animationSpec = tween(90))
                        }) { state ->
                            when (state) {
                                CommentsLoadingState.Empty -> Text(stringResource(R.string.no_comments_found))
                                CommentsLoadingState.Failed -> Text(stringResource(R.string.comments_load_failed))
                                is CommentsLoadingState.Showable ->
                                    PostComment(
                                        state.commentData,
                                        placeholder = state.commentData === CommentData.PLACEHOLDER,
                                        animateTextChange = true
                                    )

                                CommentsLoadingState.NotLoading -> Text(stringResource(R.string.click_to_load))
                                CommentsLoadingState.Forbidden -> Text(stringResource(R.string.post_comments_forbidden_auth_needed))
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                    HorizontalDivider()
                }
                item("relationships") {
                    if (post.relationships.parentId != null) TextButton(
                        onClick = component::openParentPost,
                        content = {
                            Text(stringResource(R.string.parent_post))
                            Spacer(Modifier.weight(1f))
                        }
                    )
                    if (post.relationships.hasChildren && post.relationships.children.isNotEmpty()) TextButton(
                        onClick = component::openChildrenPostListing,
                        content = {
                            Text(
                                stringResource(
                                    R.string.children_posts,
                                    post.relationships.children.size
                                )
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    )

                    if (post.pools.isNotEmpty()) {
                        TextButton(
                            onClick = component::openPools,
                            content = {
                                Text(
                                    if (post.pools.size == 1)
                                        stringResource(R.string.post_has_pool)
                                    else stringResource(R.string.post_has_pools, post.pools.size)
                                )
                                Spacer(Modifier.weight(1f))
                            }
                        )
                    }

                    if (post.relationships.hasChildren || post.relationships.parentId != null || post.pools.isNotEmpty())
                        HorizontalDivider()
                }
                item("uploaded") {
                    Spacer(Modifier.height(8.dp))
                    // TODO place more information here
                    Text(
                        stringResource(
                            R.string.uploaded_relative_date,
                            DateUtils.getRelativeTimeSpanString(
                                post.createdAt.toEpochMilliseconds(),
                                System.currentTimeMillis(),
                                SECOND_IN_MILLIS
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = BASE_PADDING_HORIZONTAL)
                    )
                }
                // Move tags to another screen?
                tags(
                    post,
                    onModificationClick = { tag, exclude ->
                        component.handleTagModification(tag, exclude)
                    },
                    onWikiClick = {
                        component.handleWikiClick(it)
                    }
                )
            }
        }

        // TODO animate fullscreen enter via fullscreen zoomable
        if (fullscreenState == FullscreenState.OPEN) {
            val file = post.selectSample()
            val initialTranslation: Offset
            val initialScale: Float
            // if image size would exceed screen height, it is true
            val matchHeightConstraintsFirst = file.aspectRatio < maxWidth / maxHeight
            if (!matchHeightConstraintsFirst) {
                initialTranslation = Offset.Zero
                initialScale = 1f
            } else {
                val width = constraints.maxHeight * file.aspectRatio
                initialScale = constraints.maxWidth / width
                initialTranslation = Offset(-(constraints.maxWidth - width) / 2, 0f)
            }
            val maxScale = initialScale * MAX_SCALE_DEFAULT
            media(
                file, post,
                Modifier
                    .zoomable(
                        rememberZoomableState(
                            maxScale = maxScale,
                            initialScale = initialScale,
                            initialTranslation = initialTranslation
                        )
                    )
                    .background(Color.Black)
                    .fillMaxSize(),
                matchHeightConstraintsFirst
            )
        }

        BackHandler(fullscreenState == FullscreenState.OPEN) {
            fullscreenState = FullscreenState.CLOSED
        }
    }

    BackHandler(enabled = bottomSheetState.currentValue != SheetValue.Hidden) {
        coroutineScope.launch {
            bottomSheetState.hide()
        }
    }
}

@Composable
fun CommentsBottomSheetContent(
    commentsFlow: Flow<PagingData<CommentData>>,
    post: Post,
    loadComments: Boolean,
) {

    val commentsLazyListState = rememberLazyListState()

    Box(
        Modifier.fillMaxSize()
    ) {
        if (!loadComments) return@Box

        val comments = commentsFlow.collectAsLazyPagingItems()
        Crossfade(
            comments.loadState.refresh is LoadState.Error,
            label = "Comments sheet animation between error and content"
        ) {
            if (it) Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Not very good
                // I'm a bad UI designer, I know
                Icon(Icons.Outlined.Error, contentDescription = null)
                Text(stringResource(R.string.comments_load_failed))
                Button(onClick = {
                    comments.retry()
                }) {
                    Text(stringResource(R.string.retry))
                }
            }
            else LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                state = commentsLazyListState,
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {}
                endOfPagePlaceholder(comments.loadState.prepend)
                items(
                    // count is known before even initialization of PagingSource, why the fuck Paging 3 does not give a way to provide it before request starts?
                    count = if (comments.loadState.refresh is LoadState.Loading) post.commentCount
                    else comments.itemCount,
                    key = { index ->
                        val comment = if (index >= comments.itemCount) null
                        else comments[index]
                        return@items comment?.id ?: "index key $index"
                    }
                    // contentType is purposely ignored as all items are of the same type and additional calls to Paging library are not needed
                ) { index ->
                    val comment =
                    // Because there is a fucking check and a fucking throw
                    // TODO migrate away from Paging 3
                        // (I think I will postpone it until workarounds are causing lags)
                        if (index >= comments.itemCount) CommentData.PLACEHOLDER
                        else comments[index] ?: CommentData.PLACEHOLDER
                    PostComment(
                        comment,
                        modifier = Modifier.padding(horizontal = BASE_PADDING_HORIZONTAL),
                        placeholder = comment === CommentData.PLACEHOLDER,
                        animateTextChange = true
                    )
                }
                endOfPagePlaceholder(comments.loadState.append)
                item {}
            }
        }
    }

}

private fun LazyListScope.tags(
    post: Post,
    onModificationClick: (tag: Tag, exclude: Boolean) -> Unit,
    onWikiClick: (Tag) -> Unit,
) {
    tags(R.string.artist_tags, post.tags.artist, onModificationClick, onWikiClick)
    tags(
        R.string.copyright_tags,
        post.tags.copyright,
        onModificationClick,
        onWikiClick
    )
    tags(
        R.string.character_tags,
        post.tags.character,
        onModificationClick,
        onWikiClick
    )
    tags(R.string.species_tags, post.tags.species, onModificationClick, onWikiClick)
    tags(R.string.general_tags, post.tags.general, onModificationClick, onWikiClick)
    tags(R.string.lore_tags, post.tags.lore, onModificationClick, onWikiClick)
    tags(R.string.meta_tags, post.tags.meta, onModificationClick, onWikiClick)
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.tags(
    @StringRes titleId: Int,
    tags: List<Tag>,
    onModificationClick: (tag: Tag, exclude: Boolean) -> Unit,
    onWikiClick: (Tag) -> Unit,
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
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0f)
                        )
                    )
                )
        ) {
            Text(
                stringResource(titleId),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
    items(tags, key = { "$it tag" }) {
        Tag(it, onModificationClick, onWikiClick)
    }
}

@Composable
private fun Tag(
    tag: Tag,
    onModificationClick: (tag: Tag, exclude: Boolean) -> Unit,
    onWikiClick: (Tag) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(tag.text, modifier = Modifier.weight(1f))
        IconButton( // Add
            onClick = {
                onModificationClick(tag, false)
            }
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add_tag_to_search)
            )
        }
        IconButton(
            onClick = {
                onModificationClick(tag, true)
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
                Icons.AutoMirrored.Filled.Help,
                contentDescription = stringResource(R.string.tag_view_wiki)
            )
        }
    }
}

enum class FullscreenState {
    OPEN,
    CLOSED,
}

private sealed interface CommentsLoadingState {
    // To be used as content key for animation
    val index: Int

    @Suppress("SpellCheckingInspection") // idk how to name it
    sealed interface Showable : CommentsLoadingState {
        override val index: Int
            get() = 2
        val commentData: CommentData

        data class Success(override val commentData: CommentData) : Showable

        data object Loading : Showable {
            override val commentData by CommentData.Companion::PLACEHOLDER
        }
    }

    data object NotLoading : CommentsLoadingState {
        override val index: Int
            get() = 0
    }

    data object Empty : CommentsLoadingState {
        override val index: Int
            get() = 1
    }

    data object Failed : CommentsLoadingState {
        override val index: Int
            get() = 3
    }

    data object Forbidden/*due to credentials absence*/ : CommentsLoadingState {
        override val index: Int
            get() = 4
    }

}