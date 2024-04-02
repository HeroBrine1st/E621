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

package ru.herobrine1st.e621.ui.component.post

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.AsyncImagePainter.State.Empty
import coil.compose.AsyncImagePainter.State.Error
import coil.compose.AsyncImagePainter.State.Loading
import coil.compose.AsyncImagePainter.State.Success
import coil.request.ImageRequest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.FileType
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.net.collectDownloadProgressAsState
import ru.herobrine1st.e621.ui.component.placeholder.PlaceholderHighlight
import ru.herobrine1st.e621.ui.component.placeholder.material3.fade
import ru.herobrine1st.e621.ui.component.placeholder.material3.placeholder

private const val TAG = "PostImage"

@Composable
fun PostImage(
    file: NormalizedFile,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    actualPostFileType: FileType? = null,
    matchHeightConstraintsFirst: Boolean = false,
    setSizeOriginal: Boolean = false,
) {
    val aspectRatio = file.aspectRatio
    val url = file.urls.firstNotNullOfOrNull { it.toHttpUrlOrNull() }

    var painterState by remember { mutableStateOf<AsyncImagePainter.State>(Empty) }
    val progress by collectDownloadProgressAsState(url)

    val context = LocalContext.current
    val imageRequest = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .apply {
                if (setSizeOriginal) size(coil.size.Size.ORIGINAL)
            }
            .build()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(
                ratio = aspectRatio.takeIf { it > 0 } ?: 1f,
                matchHeightConstraintsFirst = matchHeightConstraintsFirst
            )
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier
                .matchParentSize()
                .placeholder(
                    visible = painterState is Loading,
                    highlight = PlaceholderHighlight.fade()
                ),
            onState = {
                painterState = it
            },
            contentScale = if (aspectRatio > 0) ContentScale.Crop else ContentScale.Fit
        )
        if (actualPostFileType != null && actualPostFileType.isNotImage) AssistChip( // TODO
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 10.dp, y = 10.dp),
            colors = AssistChipDefaults.assistChipColors(
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            ),
            enabled = false,
            onClick = {},
            border = null,
            label = {
                Text(actualPostFileType.extension)
            }
        )
        when (painterState) {
            is Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Error, contentDescription = null)
                Text(stringResource(R.string.unknown_error))
            }

            is Loading -> Crossfade(
                progress == null,
                label = "Crossfade between indeterminate and determinate progress indicators"
            ) {
                when (it) {
                    true -> CircularProgressIndicator()
                    false -> {
                        val progress1 by animateFloatAsState(
                            // Non-nullability is guaranteed by collectDownloadProgressByState,
                            // which internally uses non-nullable SharedFlow
                            targetValue = progress!!.progress,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                            label = "Progress animation"
                        )
                        CircularProgressIndicator(
                            progress = { progress1 },
                        )
                    }
                }
            }

            is Success, is Empty -> {}
        }
    }
}
