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

package ru.herobrine1st.e621.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.FileType
import ru.herobrine1st.e621.navigation.component.PostMediaComponent
import ru.herobrine1st.e621.ui.component.MAX_SCALE_DEFAULT
import ru.herobrine1st.e621.ui.component.post.PostImage
import ru.herobrine1st.e621.ui.component.rememberZoomableState
import ru.herobrine1st.e621.ui.component.zoomable


@Composable
fun PostMediaScreen(
    component: PostMediaComponent,
) {
    BoxWithConstraints {
        var showOverlay by remember { mutableStateOf(false) }

        val file = component.currentFile
        val initialTranslation: Offset
        val initialScale: Float
        // if image size would exceed screen height, it is true
        val matchHeightConstraintsFirst = file.aspectRatio < maxWidth / maxHeight
        if (!matchHeightConstraintsFirst) {
            initialTranslation = Offset.Zero
            initialScale = 1f
        } else {
            val width = this.constraints.maxHeight * file.aspectRatio
            initialScale = this.constraints.maxWidth / width
            initialTranslation = Offset(-(this.constraints.maxWidth - width) * initialScale / 2, 0f)
        }
        val maxScale = initialScale * MAX_SCALE_DEFAULT

        val modifier = Modifier
            .zoomable(
                state = rememberZoomableState(
                    maxScale = maxScale,
                    initialScale = initialScale,
                    initialTranslation = initialTranslation
                ),
                onTap = { showOverlay = !showOverlay }
            )
            .background(Color.Black)
            .fillMaxSize()

        Box {
            when {
                file.type.isVideo -> {
                    // Not supported
                }

                file.type.isImage -> PostImage(
                    file = file,
                    contentDescription = null,
                    modifier = modifier,
                    actualPostFileType = null,
                    matchHeightConstraintsFirst = matchHeightConstraintsFirst,
                    setSizeOriginal = true
                )

                else -> {}
            }
            AnimatedVisibility(
                visible = showOverlay,
                enter = fadeIn() + slideInVertically(),
                exit = slideOutVertically() + fadeOut()
            ) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    Row(
                        Modifier
                            .background(Color.Black.copy(alpha = 0.5f))
                            .systemBarsPadding()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            file.name.replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.weight(1f))
                        Box {
                            var showMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.image_fullscreen_select_image_variant)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.download)) },
                                    leadingIcon = { Icon(Icons.Default.Download, null) },
                                    onClick = { component.downloadFile() }
                                )
                                HorizontalDivider()
                                component.files.forEach {
                                    DropdownMenuItem(
                                        text = {
                                            Text(it.name.replaceFirstChar { it.uppercase() })
                                        },
                                        leadingIcon = {
                                            val icon = when (it.type) {
                                                FileType.JPG, FileType.PNG -> Icons.Default.Image
                                                FileType.GIF -> Icons.Default.Animation
                                                FileType.WEBM, FileType.MP4 -> Icons.Default.Movie
                                                FileType.SWF -> return@DropdownMenuItem
                                            }
                                            Icon(icon, null)
                                        },
                                        onClick = {
                                            component.setFile(it)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}