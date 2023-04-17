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
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.AsyncImagePainter.State.*
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.FileType
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.net.collectDownloadProgressAsState
import ru.herobrine1st.e621.util.debug

private const val TAG = "PostImage"

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PostImage(
    file: NormalizedFile,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    actualPostFileType: FileType? = null
) {
    val aspectRatio = file.aspectRatio
    val url = file.urls.firstNotNullOfOrNull { it.toHttpUrlOrNull() }

    var painterState by remember { mutableStateOf<AsyncImagePainter.State>(Empty) }
    val progress by collectDownloadProgressAsState(url)

    debug {
        var maxProgress by remember { mutableStateOf(progress) }
        LaunchedEffect(progress?.progress) {
            progress?.let { progress ->
                if (progress.progress < (maxProgress?.progress ?: 0f)) {
                    Log.w(
                        TAG,
                        "Progress for ${progress.url} went backwards: from $maxProgress to $progress"
                    )
                } else {
                    maxProgress = progress
                }
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(aspectRatio.takeIf { it > 0 } ?: 1f)
    ) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = Modifier
                .matchParentSize()
                .placeholder(
                    visible = painterState is Loading || painterState is Empty,
                    highlight = PlaceholderHighlight.fade()
                ),
            onState = {
                painterState = it
            }
        )
        // FIXME now it is always visible..
        // it is always visible because of fix which fixed that is was always NOT visible
        // ironic..
        if (actualPostFileType != null && actualPostFileType != file.type) Chip( // TODO
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 10.dp, y = 10.dp),
            colors = ChipDefaults.outlinedChipColors(
                backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.3f)
            ),
            enabled = false,
            onClick = {}
        ) {
            Text(actualPostFileType.extension)
        }
        when (painterState) {
            is Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Error, contentDescription = null)
                Text(stringResource(R.string.unknown_error))
            }
            is Loading, Empty -> Crossfade(progress == null) {
                when (it) {
                    true -> CircularProgressIndicator()
                    false -> CircularProgressIndicator(
                        animateFloatAsState(
                            // Non-nullability is guaranteed by collectDownloadProgressByState,
                            // which internally uses non-nullable SharedFlow
                            targetValue = progress!!.progress,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                        ).value
                    )
                }
            }
            else -> {}
        }
    }
}
