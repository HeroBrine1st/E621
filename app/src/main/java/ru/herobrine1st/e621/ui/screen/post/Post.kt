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
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.*
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.selectSample
import ru.herobrine1st.e621.navigation.component.post.PostComponent
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.ui.component.*
import ru.herobrine1st.e621.ui.component.post.PostMediaContainer
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState
import ru.herobrine1st.e621.ui.screen.post.component.GoingToFullscreenAnimation
import ru.herobrine1st.e621.ui.screen.post.component.PostComment
import ru.herobrine1st.e621.ui.screen.post.component.PostCommentPlaceholder
import ru.herobrine1st.e621.ui.screen.post.data.CommentData
import ru.herobrine1st.e621.util.normalizeTagForUI
import java.util.*

private const val DESCRIPTION_COLLAPSED_HEIGHT_FRACTION = 0.4f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Post(
    screenSharedState: ScreenSharedState,
    component: PostComponent
) {
    val post = component.post
    val preferences = LocalPreferences.current

    val coroutineScope = rememberCoroutineScope()

    var loadComments by remember { mutableStateOf(!preferences.dataSaverModeEnabled || component.openComments) } // Do not make excessive API calls (user preference)

    if (post == null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        return
    }

    var fullscreenState by remember { mutableStateOf(FullscreenState.CLOSE) }

    var imagePositionInRoot by remember { mutableStateOf(Offset.Unspecified) }

    val media = remember {
        movableContentOf { file: NormalizedFile, post: Post, modifier: Modifier ->
            PostMediaContainer(
                file = file,
                contentDescription = remember(post.id) { post.tags.all.joinToString(" ") },
                modifier = modifier
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
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
            // PaddingValues purposely unused because it clutters screen if applied via modifier
            // and lags if applied via contentPadding
            // while has no top padding, so useless
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                userScrollEnabled = fullscreenState == FullscreenState.CLOSE
            ) {
                item("media") {
                    val sample = post.selectSample()

                    // "Open to fullscreen" behavior
                    // It is obvious that this code is bad from its size (it is not all, the
                    // second part is below, and also in another file)
                    // It is not really fullscreen - appbar is still visible
                    // Still VIP, maybe Window should be used
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
                                imagePositionInRoot = it.positionInRoot()
                            }
                    ) {
                        if (fullscreenState == FullscreenState.CLOSE)
                            media(sample, post, Modifier.fillMaxWidth())
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
                        if (post.commentCount == 0) {
                            Text(stringResource(R.string.no_comments_found))
                        } else if (loadComments) {
                            val comments = component.commentsFlow.collectAsLazyPagingItems()
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
        GoingToFullscreenAnimation(
            isFullscreen = fullscreenState == FullscreenState.OPEN,
            contentAspectRatio = post.selectSample().aspectRatio,
            getContentPositionRelativeToParent = { imagePositionInRoot },
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
                media(post.selectSample(), post, Modifier.fillMaxSize())
            }
        }

        BackHandler(fullscreenState == FullscreenState.OPEN) {
            fullscreenState = FullscreenState.CLOSING
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
    loadComments: Boolean
) {

    val commentsLazyListState = rememberLazyListState()

    if (!loadComments) {
        Box(Modifier.fillMaxSize()) // Set size so that drawer won't auto-expand when opened
        return
    }

    val comments = commentsFlow.collectAsLazyPagingItems()

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        state = commentsLazyListState,
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {}
        // TODO manage placeholders with Jetpack Paging
        if (comments.loadState.refresh is LoadState.Loading) {
            items(count = post.commentCount) {
                PostCommentPlaceholder(modifier = Modifier.padding(horizontal = BASE_PADDING_HORIZONTAL))
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
        }
        endOfPagePlaceholder(comments.loadState.append)
        item {}
    }
}

private fun LazyListScope.tags(
    post: Post,
    onModificationClick: (tag: String, exclude: Boolean) -> Unit,
    onWikiClick: (String) -> Unit
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
    tags: List<String>,
    onModificationClick: (tag: String, exclude: Boolean) -> Unit,
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
    tag: String,
    onModificationClick: (tag: String, exclude: Boolean) -> Unit,
    onWikiClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(tag.normalizeTagForUI(), modifier = Modifier.weight(1f))
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