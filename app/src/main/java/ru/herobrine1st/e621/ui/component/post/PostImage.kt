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

package ru.herobrine1st.e621.ui.component.post

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import okhttp3.HttpUrl.Companion.toHttpUrl
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.net.collectDownloadProgressAsState
import ru.herobrine1st.e621.util.debug

private const val TAG = "PostImage"

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PostImage(
    post: Post,
    openPost: (() -> Unit)?,
    file: NormalizedFile,
    modifier: Modifier = Modifier,
    initialAspectRatio: Float = file.aspectRatio
) {
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var aspectRatio by remember { mutableStateOf(initialAspectRatio) }

    val url = remember(file) { file.urls.first().toHttpUrl() }
    val progress by collectDownloadProgressAsState(url)

    debug {
        var maxProgress by remember { mutableStateOf(progress.progress) }
        LaunchedEffect(progress.progress) {
            if(progress.progress < maxProgress) {
                Log.w(TAG, "Progress for $url went backwards: from $maxProgress to ${progress.progress}")
            } else maxProgress = progress.progress
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        AsyncImage(
            model = url,
            modifier = Modifier
                .clickable(openPost != null) {
                    openPost?.invoke()
                }
                .fillMaxWidth()
                .aspectRatio(aspectRatio.takeIf { it > 0 } ?: 1f)
                .placeholder(isLoading, highlight = PlaceholderHighlight.fade()),
            onSuccess = {
                isLoading = false
                isError = false
                aspectRatio = it.result.drawable.run { intrinsicWidth.toFloat() / intrinsicHeight }
            },
            onError = {
                isLoading = false
                isError = true
            },
            contentDescription = remember(post.id) { post.tags.all.joinToString(" ") },
            contentScale = ContentScale.Fit
        )
        if (post.file.type.isNotImage) Chip( // TODO
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 10.dp, y = 10.dp),
            colors = ChipDefaults.outlinedChipColors(
                backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f)
            ),
            enabled = false,
            onClick = {}
        ) {
            Text(post.file.type.extension)
        }
        when {
            isError -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Error, contentDescription = null)
                Text(stringResource(R.string.unknown_error))
            }
            isLoading -> Crossfade(progress.isValid) {
                when (it) {
                    false -> CircularProgressIndicator()
                    true -> CircularProgressIndicator(
                        animateFloatAsState(
                            targetValue = progress.progress,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                        ).value
                    )
                }
            }
        }
    }
}

