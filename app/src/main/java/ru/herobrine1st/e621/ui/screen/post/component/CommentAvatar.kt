/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.ui.screen.post.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import ru.herobrine1st.e621.api.model.PostReduced

@Composable
@OptIn(ExperimentalCoilApi::class)
fun CommentAvatar(avatarPost: PostReduced?, modifier: Modifier = Modifier, placeholder: Boolean = false) {
    val url = avatarPost?.previewUrl ?: avatarPost?.croppedUrl
    if (url != null) {
        val imagePainter = rememberImagePainter(url) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        Image(
            painter = imagePainter,
            modifier = modifier
                .clip(CircleShape) // For placeholder
                .placeholder(
                    imagePainter.state is ImagePainter.State.Loading,
                    highlight = PlaceholderHighlight.fade()
                ),
            contentDescription = null
        )
    } else {
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = null,
            // If placeholder = true, there should be no avatarPost object provided (=null)
            modifier = modifier
                .clip(CircleShape) // For placeholder
                .placeholder(placeholder, highlight = PlaceholderHighlight.fade())
        )
    }
}