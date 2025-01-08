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

package ru.herobrine1st.e621.ui.screen.post.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import ru.herobrine1st.e621.api.common.AvatarImageCommon
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.ui.component.placeholder.PlaceholderHighlight
import ru.herobrine1st.e621.ui.component.placeholder.material3.fade
import ru.herobrine1st.e621.ui.component.placeholder.material3.placeholder

@Composable
fun CommentAvatar(
    avatarPost: AvatarImageCommon?,
    safeModeEnabled: Boolean,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
    onAvatarClick: () -> Unit = {}
) {
    var isPlaceholderActive by remember { mutableStateOf(true) }
    if (avatarPost != null && (avatarPost.rating == Rating.SAFE || !safeModeEnabled)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarPost.url)
                .crossfade(true)
                .transformations(CircleCropTransformation()) // clip(CircleShape) is wrong (white/black zones are present)
                .build(),
            onSuccess = {
                isPlaceholderActive = false
            },
            onError = {
                isPlaceholderActive = false
            },
            modifier = modifier
                .clip(CircleShape) // For placeholder
                .clickable(onClick = onAvatarClick)
                .placeholder(
                    isPlaceholderActive,
                    highlight = PlaceholderHighlight.fade()
                ),
            contentDescription = null
        )
    } else {
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = null,
            // If placeholder = true, there should be no url provided (=null)
            modifier = modifier
                .clip(CircleShape) // For placeholder
                .placeholder(placeholder, highlight = PlaceholderHighlight.fade())
        )
    }
}