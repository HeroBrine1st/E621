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

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.util.text

fun LazyListScope.tags(
    post: Post,
    onModificationClick: (tag: Tag, exclude: Boolean) -> Unit,
    onWikiClick: (Tag) -> Unit,
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
fun LazyListScope.tags(
    @StringRes titleId: Int,
    tags: List<Tag>,
    onModificationClick: (tag: Tag, exclude: Boolean) -> Unit,
    onWikiClick: (Tag) -> Unit,
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
fun Tag(
    tag: Tag,
    onModificationClick: (tag: Tag, exclude: Boolean) -> Unit,
    onWikiClick: (Tag) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(tag.text, modifier = Modifier.weight(1f))
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
                Icons.AutoMirrored.Filled.Help,
                contentDescription = stringResource(R.string.tag_view_wiki)
            )
        }
    }
}