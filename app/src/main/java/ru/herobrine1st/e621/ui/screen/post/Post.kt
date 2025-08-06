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

package ru.herobrine1st.e621.ui.screen.post

import android.text.format.DateUtils
import android.text.format.DateUtils.SECOND_IN_MILLIS
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.common.CommentData
import ru.herobrine1st.e621.api.model.isOriginal
import ru.herobrine1st.e621.module.CachedDataStore
import ru.herobrine1st.e621.navigation.component.post.PostComponent
import ru.herobrine1st.e621.navigation.component.post.PostState
import ru.herobrine1st.e621.navigation.component.posts.TransientPost
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.component.CollapsibleColumn
import ru.herobrine1st.e621.ui.component.RenderBB
import ru.herobrine1st.e621.ui.component.post.InvalidPost
import ru.herobrine1st.e621.ui.component.post.PostActionRow
import ru.herobrine1st.e621.ui.component.post.PostImage
import ru.herobrine1st.e621.ui.component.post.PostVideo
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState
import ru.herobrine1st.e621.ui.screen.post.component.PoolsDialog
import ru.herobrine1st.e621.ui.screen.post.component.PostComment
import ru.herobrine1st.e621.ui.screen.post.component.tags
import ru.herobrine1st.paging.api.LoadState
import ru.herobrine1st.paging.api.collectAsPagingItems
import kotlin.time.ExperimentalTime

private const val DESCRIPTION_COLLAPSED_HEIGHT_FRACTION = 0.4f

@OptIn(ExperimentalMaterial3Api::class, CachedDataStore::class, ExperimentalTime::class)
@Composable
fun Post(
    screenSharedState: ScreenSharedState,
    component: PostComponent
) {
    val postState by component.postState.subscribeAsState()
    val preferences by component.preferences.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // Probably can't use refresh state to replace this variable at first frame
    val loadComments =
        preferences.auth != null // Assuming there can't be invalid credentials in preferences
                && (
                !preferences.dataSaverModeEnabled // Do not make excessive API calls on user preference
                        || component.openComments)
    val comments = component.commentsFlow.collectAsPagingItems(startImmediately = loadComments)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val bottomSheetState = rememberStandardBottomSheetState(
        // `&& loadComments` is a fix for unauthenticated usage case
        // Think of it as there is no point in opening comments if they're not loading
        initialValue = if (component.openComments && loadComments) SheetValue.PartiallyExpanded
        else SheetValue.Hidden,
        skipHiddenState = false
    )

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState,
        snackbarHostState = screenSharedState.snackbarHostState
    )

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
                            onOpenBlacklistDialog = screenSharedState.openBlacklistDialog,
                            additionalMenuActions = {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.download)) },
                                    leadingIcon = { Icon(Icons.Default.Download, null) },
                                    onClick = { component.downloadFile() }
                                )
                            }
                        )
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            sheetContent = {
                (postState as? PostState.Ready)?.post?.let { post ->
                    CommentsBottomSheetContent(
                        comments = comments,
                        post = post,
                        safeModeEnabled = preferences.safeModeEnabled
                    )
                }
            },
            sheetPeekHeight = maxHeight * 0.5f,
            scaffoldState = bottomSheetScaffoldState,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { _ ->
            val post = (postState as? PostState.Ready)?.post
            if (post == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(Modifier.weight(1f))
                    if (postState is PostState.Loading) CircularProgressIndicator()
                    else {
                        Icon(Icons.Outlined.Error, contentDescription = null)
                        Text(stringResource(R.string.could_not_load_post))
                        Button(onClick = {
                            component.refreshPost()
                        }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
                return@BottomSheetScaffold
            }


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

            // provided PaddingValues aren't connected to actual visibility window, so are ignored
            // handling insets manually
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = with(LocalDensity.current) {
                    PaddingValues(
                        bottom = WindowInsets.navigationBars.getBottom(this).toDp()
                    )
                },
                modifier = Modifier.consumeWindowInsets(
                    WindowInsets.navigationBars.only(
                        WindowInsetsSides.Bottom
                    )
                )
            ) {
                item("media") {
                    val file = component.currentFile

                    if(file != null) {
                        // "Open to fullscreen" behavior
                        // Currently no animation
                        Box(
                            Modifier
                                // This box will be empty if image is gone to fullscreen
                                .aspectRatio(file.aspectRatio)
                                .fillMaxWidth()
                                .clickable(
                                    enabled = !post.file.type.isVideo,
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    component.openToFullscreen()
                                }
                        ) {
                            when {
                                file.type.isVideo -> PostVideo(
                                    component.getVideoPlayerComponent(file),
                                    file,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                file.type.isImage -> PostImage(
                                    file = file,
                                    contentDescription = remember(post.id) {
                                        post.tags.all.joinToString(
                                            " "
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    actualPostFileType = post.file.type,
                                    setSizeOriginal = true // to be loaded instantly
                                )

                                else -> InvalidPost(
                                    text = stringResource(
                                        R.string.unsupported_post_type,
                                        file.type.extension
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                item("actionbar") {
                    val favouriteState by component.isFavouriteAsState()
                    PostActionRow(
                        remember(post) { TransientPost(post) },
                        favouriteState,
                        component.isAuthorized,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth(),
                        onFavouriteChange = component::handleFavouriteChange,
                        onOpenComments = {
                            coroutineScope.launch {
                                if (comments.loadStates.refresh is LoadState.NotLoading) {
                                    comments.refresh()
                                }
                                bottomSheetState.partialExpand()
                            }
                        },
                        onVote = {
                            component.vote(post.id, it)
                        },
                        getVote = {
                            component.getVote(post.id) ?: 0
                        }
                    )
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
                            .clickable(enabled = post.commentCount != 0 && preferences.auth != null) {
                                coroutineScope.launch {
                                    if (comments.loadStates.refresh is LoadState.NotLoading) {
                                        comments.refresh()
                                    }
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
                            post.commentCount == 0 -> CommentsLoadingState.Empty

                            comments.loadStates.refresh !is LoadState.NotLoading -> {
                                when (comments.loadStates.refresh) {
                                    is LoadState.Loading -> CommentsLoadingState.Showable.Loading

                                    is LoadState.Complete ->
                                        if (comments.items.isEmpty()) CommentsLoadingState.Empty
                                        else CommentsLoadingState.Showable.Success(comments.peek(0))

                                    // Paging is not initialized yet
                                    is LoadState.Idle -> CommentsLoadingState.NotLoading


                                    is LoadState.Error, is LoadState.NotLoading -> CommentsLoadingState.Failed
                                }
                            }

                            preferences.auth == null -> CommentsLoadingState.Forbidden

                            else -> CommentsLoadingState.NotLoading
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
                                        safeModeEnabled = preferences.safeModeEnabled,
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
                        val poolsComponent = component.poolsComponent
                        PoolsDialog(poolsComponent)
                        TextButton(
                            onClick = poolsComponent::activate,
                            content = {
                                Text(
                                    if (poolsComponent.pools.size == 1)
                                        stringResource(R.string.post_has_pool)
                                    else stringResource(
                                        R.string.post_has_pools,
                                        poolsComponent.pools.size
                                    )
                                )
                                Spacer(Modifier.weight(1f))
                                if (poolsComponent.pools.size == 1) Crossfade(
                                    poolsComponent.loadingPools,
                                    label = "Crossfade between icon and loading"
                                ) {
                                    // 24.dp is size of icon
                                    // strokeWidth is decreased in proportion (24/40 * 4)
                                    if (it) CircularProgressIndicator(
                                        Modifier.size(24.dp),
                                        strokeWidth = 2.4.dp
                                    )
                                    else Icon(
                                        Icons.AutoMirrored.Default.NavigateNext,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }

                    if (post.relationships.hasChildren || post.relationships.parentId != null || post.pools.isNotEmpty())
                        HorizontalDivider()
                }
                item("show original file button") {
                    TextButton(
                        onClick = { component.setFile(post.normalizedFile) },
                        enabled = component.currentFile?.isOriginal() != true
                    ) {
                        Text(stringResource(R.string.show_original_file))
                        Spacer(Modifier.weight(1f))
                    }
                    HorizontalDivider()
                }
                item("stats") {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = BASE_PADDING_HORIZONTAL)
                    ) {
                        Text(
                            stringResource(
                                R.string.uploaded_relative_date,
                                DateUtils.getRelativeTimeSpanString(
                                    post.createdAt.toEpochMilliseconds(),
                                    System.currentTimeMillis(),
                                    SECOND_IN_MILLIS
                                )
                            )
                        )
                        if (post.updatedAt != null)
                            Text(
                                stringResource(
                                    R.string.post_updated, DateUtils.getRelativeTimeSpanString(
                                        post.updatedAt.toEpochMilliseconds(),
                                        System.currentTimeMillis(),
                                        SECOND_IN_MILLIS
                                    )
                                )
                            )

                        Text(
                            stringResource(
                                R.string.post_rating,
                                stringResource(post.rating.descriptionId)
                            )
                        )
                        if (post.flags.pending) Text(stringResource(R.string.post_is_pending))
                        if (post.hasNotes) Text(stringResource(R.string.post_has_notes))
                    }

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
    }

    BackHandler(enabled = bottomSheetState.currentValue != SheetValue.Hidden) {
        coroutineScope.launch {
            bottomSheetState.hide()
        }
    }
}