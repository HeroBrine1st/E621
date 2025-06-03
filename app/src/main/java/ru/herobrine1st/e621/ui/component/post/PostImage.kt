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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePainter.State.*
import coil3.request.ImageRequest
import io.ktor.client.content.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.FileType
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.ui.component.placeholder.PlaceholderHighlight
import ru.herobrine1st.e621.ui.component.placeholder.material3.fade
import ru.herobrine1st.e621.ui.component.placeholder.material3.placeholder
import ru.herobrine1st.e621.util.progressCallbackExtra

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

    var painterState by remember { mutableStateOf<AsyncImagePainter.State>(Empty) }
    var progress by remember { mutableFloatStateOf(-1f) }

    val context = LocalContext.current
    val imageRequest = remember(file.urls) {
        ImageRequest.Builder(context)
            // toHttpUrlOrNull - because toUri does not throw
            .data(file.urls.first { it.toHttpUrlOrNull() != null }.toUri())
            .apply {
                if (setSizeOriginal) size(coil3.size.Size.ORIGINAL)
                extras[progressCallbackExtra] = ProgressListener { received, total ->
                    if (total != null) progress = received.toFloat() / total
                }
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
                progress < 0,
                label = "Crossfade between indeterminate and determinate progress indicators"
            ) {
                when (it) {
                    true -> CircularProgressIndicator()
                    false -> {
                        val progress1 by animateFloatAsState(
                            targetValue = progress,
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
