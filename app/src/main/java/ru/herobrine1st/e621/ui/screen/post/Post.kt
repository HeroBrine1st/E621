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

import android.app.Activity
import android.text.format.DateUtils
import android.text.format.DateUtils.SECOND_IN_MILLIS
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.api.SearchOptions
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.selectSample
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.ui.component.*
import ru.herobrine1st.e621.ui.component.post.PostMediaContainer
import ru.herobrine1st.e621.ui.dialog.ContentDialog
import ru.herobrine1st.e621.ui.screen.post.component.GoingToFullscreenAnimation
import ru.herobrine1st.e621.ui.screen.post.component.PostComment
import ru.herobrine1st.e621.ui.screen.post.component.PostCommentPlaceholder
import ru.herobrine1st.e621.ui.screen.post.data.CommentData
import ru.herobrine1st.e621.ui.screen.post.logic.PostViewModel
import ru.herobrine1st.e621.ui.screen.post.logic.WikiResult
import ru.herobrine1st.e621.util.normalizeTagForUI
import java.util.*

private const val TAG = "Post Screen"
private const val DESCRIPTION_COLLAPSED_HEIGHT_FRACTION = 0.4f

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun Post(
    id: Int,
    initialPost: Post?,
    openComments: Boolean,
    searchOptions: SearchOptions,
    onModificationClick: (PostsSearchOptions) -> Unit,
    viewModel: PostViewModel = viewModel(
        factory = PostViewModel.provideFactory(
            EntryPointAccessors.fromActivity<PostViewModel.FactoryProvider>(
                LocalContext.current as Activity
            ).provideFactory(), id, initialPost
        )
    )
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val post = viewModel.post
    val wikiState = viewModel.wikiState
    val preferences = LocalPreferences.current

    val coroutineScope = rememberCoroutineScope()

    val drawerState = rememberBottomDrawerState(
        initialValue = if (openComments) BottomDrawerValue.Expanded // Opened works here
        else BottomDrawerValue.Closed
    )

    var loadComments by remember { mutableStateOf(!preferences.privacyModeEnabled || openComments) } // Do not make excessive API calls (user preference)

    if (post == null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (wikiState != null) ContentDialog(
        title = wikiState.title.replaceFirstChar { // Capitalize
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        },
        onDismissRequest = { viewModel.closeWikiPage() }
    ) {
        LazyColumn(
            modifier = Modifier.height(screenHeight * 0.4f)
        ) {
            when (wikiState) {
                is WikiResult.Loading -> items(50) {
                    Text(
                        "", modifier = Modifier
                            .fillMaxWidth()
                            .placeholder(true, highlight = PlaceholderHighlight.shimmer())
                    )
                    Spacer(Modifier.height(4.dp))
                }
                is WikiResult.Failure -> item {
                    Text(stringResource(R.string.wiki_load_failed))
                }
                is WikiResult.NotFound -> item {
                    Text(stringResource(R.string.not_found))
                }
                is WikiResult.Success -> item {
                    Text(wikiState.result.body)
                }
            }
        }
    }

    var fullscreenState by remember { mutableStateOf(FullscreenState.CLOSE) }

    var imagePositionInParent by remember { mutableStateOf(Offset.Unspecified) }

    val media = remember {
        movableContentOf { file: NormalizedFile ->
            PostMediaContainer(
                file = file,
                contentDescription = remember(post.id) { post.tags.all.joinToString(" ") }
            )
        }
    }

    CommentsBottomDrawer(
        drawerState = drawerState,
        commentsFlow = viewModel.commentsFlow,
        post = post,
        loadComments = loadComments,
    ) {
        BoxWithConstraints {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                userScrollEnabled = fullscreenState == FullscreenState.CLOSE
            ) {
                item("media") {
                    val sample = post.selectSample()

                    // "Open to fullscreen" behavior
                    // It is obvious that this code is bad from its size (it is not all, the
                    // second part is below, and also in another file)
                    // It is not really fullscreen - status bar is still visible
                    // Still VIP, maybe Window is should be used
                    // Or navigation should handle this. As AndroidX Compose Navigation is
                    // not flexible, I look at Decompose
                    Box(
                        Modifier
                            // This box will be empty if image is gone to fullscreen
                            .aspectRatio(sample.aspectRatio)
                            .fillMaxWidth()
                            .clickable(
                                enabled = fullscreenState == FullscreenState.CLOSE
                                        && !post.file.type.isVideo,
                                indication = null, // The animation is the indication
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                fullscreenState = FullscreenState.OPEN
                            }
                            .onGloballyPositioned {
                                imagePositionInParent = it.positionInParent()
                            }
                    ) {
                        if (fullscreenState == FullscreenState.CLOSE)
                            media(sample)
                    }
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
                            style = MaterialTheme.typography.h6
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
                                    val rotation: Float by animateFloatAsState(if (expanded) 180f else 360f)
                                    Icon(
                                        Icons.Default.ExpandMore, null, modifier = Modifier
                                            .padding(start = 4.dp, end = 12.dp)
                                            .rotate(rotation)
                                    )
                                    Crossfade(expanded) { state ->
                                        Text(
                                            stringResource(if (!state) R.string.expand else R.string.collapse),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        ) {
                            RenderBB(post.description)
                        }
                    }
                    Divider()
                }
                item("comments") {
                    Column(Modifier
                        .fillMaxWidth()
                        .clickable(enabled = post.commentCount != 0) {
                            coroutineScope.launch {
                                loadComments = true
                                drawerState.open()
                            }
                        }
                        .padding(horizontal = BASE_PADDING_HORIZONTAL)
                    ) {
                        Text(
                            stringResource(R.string.comments),
                            style = MaterialTheme.typography.h6
                        )
                        Spacer(Modifier.width(4.dp))
                        if (post.commentCount == 0) {
                            Text(stringResource(R.string.no_comments_found))
                        } else if (loadComments) {
                            val comments = viewModel.commentsFlow.collectAsLazyPagingItems()
                            when (comments.loadState.refresh) {
                                is LoadState.Loading -> {
                                    PostCommentPlaceholder()
                                }
                                is LoadState.NotLoading -> {
                                    if (comments.itemSnapshotList.isEmpty()) {
                                        Text(stringResource(R.string.no_comments_found))
                                    } else {
                                        val comment = comments.peek(0)
                                        if (comment != null) PostComment(comment)
                                        else Text(stringResource(R.string.no_comments_found))
                                    }
                                }
                                is LoadState.Error -> Text(stringResource(R.string.comments_load_failed))
                            }
                        } else {
                            Text(stringResource(R.string.click_to_load))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Divider()
                }
                item("uploaded") {
                    // TODO place more information here
                    Text(
                        stringResource(
                            R.string.uploaded_relative_date,
                            DateUtils.getRelativeTimeSpanString(
                                post.createdAt.toEpochSecond() * 1000,
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
                tags(post, searchOptions, onModificationClick, onWikiClick = {
                    viewModel.handleWikiClick(it)
                })
            }

            GoingToFullscreenAnimation(
                isFullscreen = fullscreenState == FullscreenState.OPEN,
                contentAspectRatio = post.selectSample().aspectRatio,
                getContentPositionRelativeToParent = { imagePositionInParent },
                assumeTranslatedComponentIsInFullscreenContainerCentered = true,
                onExit = {
                    fullscreenState = FullscreenState.CLOSE
                },
                componentBackground = {
                    Surface(
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                            indication = null, // The animation is the indication
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            fullscreenState = FullscreenState.CLOSING
                        }
                    ) {}
                }
            ) {
                Zoomable(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    media(post.selectSample())
                }
            }

            BackHandler(fullscreenState == FullscreenState.OPEN) {
                fullscreenState = FullscreenState.CLOSING
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CommentsBottomDrawer(
    drawerState: BottomDrawerState,
    commentsFlow: Flow<PagingData<CommentData>>,
    post: Post,
    loadComments: Boolean,
    content: @Composable () -> Unit
) {

    val commentsLazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isExpanded by remember(drawerState) {
        derivedStateOf {
            drawerState.progress.fraction == 1f && drawerState.progress.to == BottomDrawerValue.Expanded
        }
    }

    val elevation by animateDpAsState(if (isExpanded) 0.dp else AppBarDefaults.TopAppBarElevation)
    val shapeSize by animateDpAsState(if (isExpanded) 0.dp else 8.dp)
    val drawerBackgroundColor by animateColorAsState(if (isExpanded) MaterialTheme.colors.background else MaterialTheme.colors.surface)

    val isGesturesEnabled by remember(drawerState, commentsLazyListState) {
        derivedStateOf {
            !drawerState.isClosed // Disallow opening by gesture
                    // Disallow closing when scrolled down
                    && commentsLazyListState.firstVisibleItemIndex == 0
                    && commentsLazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    BottomDrawer(
        drawerState = drawerState,
        gesturesEnabled = isGesturesEnabled,
        drawerShape = RoundedCornerShape(shapeSize),
        scrimColor = Color.Transparent,
        drawerBackgroundColor = drawerBackgroundColor,
        drawerContent = {
            // Do not load comments while drawer is closed
            if (!loadComments) {
                Box(Modifier.fillMaxSize()) // Set size so that drawer won't auto-expand when opened
                return@BottomDrawer
            }
            val comments = commentsFlow.collectAsLazyPagingItems()
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                state = commentsLazyListState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = isExpanded
            ) {
                item {
                    TopAppBar(backgroundColor = MaterialTheme.colors.surface,
                        elevation = elevation,
                        title = {
                            Text(stringResource(R.string.comments))
                        })
                }
                item { Spacer(Modifier.height(8.dp)) }
                if (comments.loadState.refresh is LoadState.Loading) {
                    items(count = post.commentCount) {
                        PostCommentPlaceholder(modifier = Modifier.padding(horizontal = BASE_PADDING_HORIZONTAL))
                        Spacer(Modifier.height(8.dp))
                    }
                    return@LazyColumn
                }
                endOfPagePlaceholder(comments.loadState.prepend)
                items(comments, key = { it.id }) { comment ->
                    if (comment == null) return@items
                    if (comment.isHidden) return@items
                    PostComment(
                        comment,
                        modifier = Modifier.padding(horizontal = BASE_PADDING_HORIZONTAL)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                endOfPagePlaceholder(comments.loadState.append)
            }
        }
    ) {
        content()
    }

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch {
            drawerState.close()
            commentsLazyListState.scrollToItem(0)
        }
    }
}

fun LazyListScope.tags(
    post: Post,
    searchOptions: SearchOptions,
    onModificationClick: (PostsSearchOptions) -> Unit,
    onWikiClick: (String) -> Unit
) {
    tags(R.string.artist_tags, post.tags.artist, searchOptions, onModificationClick, onWikiClick)
    tags(
        R.string.copyright_tags,
        post.tags.copyright,
        searchOptions,
        onModificationClick,
        onWikiClick
    )
    tags(
        R.string.character_tags,
        post.tags.character,
        searchOptions,
        onModificationClick,
        onWikiClick
    )
    tags(R.string.species_tags, post.tags.species, searchOptions, onModificationClick, onWikiClick)
    tags(R.string.general_tags, post.tags.general, searchOptions, onModificationClick, onWikiClick)
    tags(R.string.lore_tags, post.tags.lore, searchOptions, onModificationClick, onWikiClick)
    tags(R.string.meta_tags, post.tags.meta, searchOptions, onModificationClick, onWikiClick)
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.tags(
    @StringRes titleId: Int,
    tags: List<String>,
    searchOptions: SearchOptions,
    onModificationClick: (PostsSearchOptions) -> Unit,
    onWikiClick: (String) -> Unit
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
                            MaterialTheme.colors.background,
                            MaterialTheme.colors.background.copy(alpha = 0f)
                        )
                    )
                )
        ) {
            Text(
                stringResource(titleId),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.weight(1f)
            )
        }
    }
    items(tags, key = { "$it tag" }) {
        Tag(it, searchOptions, onModificationClick, onWikiClick)
    }
}

@Composable
fun Tag(
    tag: String,
    searchOptions: SearchOptions,
    onModificationClick: (PostsSearchOptions) -> Unit,
    onWikiClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(tag.normalizeTagForUI(), modifier = Modifier.weight(1f))
        IconButton( // Add
            onClick = {
                onModificationClick(searchOptions.toBuilder { tags += tag })
            }
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add_tag_to_search)
            )
        }
        IconButton(
            onClick = {
                onModificationClick(searchOptions.toBuilder { tags += "-$tag" })
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
                Icons.Default.Help,
                contentDescription = stringResource(R.string.tag_view_wiki)
            )
        }
    }
}

enum class FullscreenState {
    OPEN,
    CLOSE,
    CLOSING
}