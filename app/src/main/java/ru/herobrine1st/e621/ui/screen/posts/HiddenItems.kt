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

package ru.herobrine1st.e621.ui.screen.posts

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.posts.PostListingItem

@Composable
fun HiddenItems(item: PostListingItem.HiddenItems) {
    val modifier = Modifier.padding(horizontal = 8.dp)
    if (item.hiddenDueToSafeModeNumber == 0) {
        Text(
            stringResource(
                R.string.posts_hidden_blacklisted,
                pluralStringResource(
                    id = R.plurals.list_items,
                    count = item.hiddenDueToBlacklistNumber,
                    item.hiddenDueToBlacklistNumber
                )
            ), modifier
        )
    } else if (item.hiddenDueToBlacklistNumber == 0) {
        Text(
            stringResource(
                R.string.posts_hidden_safe_mode,
                pluralStringResource(
                    id = R.plurals.list_items,
                    count = item.hiddenDueToSafeModeNumber,
                    item.hiddenDueToSafeModeNumber
                )
            ), modifier
        )
    } else {
        Text(
            stringResource(
                R.string.posts_hidden_blacklist_and_safe_mode,
                pluralStringResource(
                    id = R.plurals.list_items,
                    count = item.hiddenDueToSafeModeNumber,
                    item.hiddenDueToSafeModeNumber
                ),
                item.hiddenDueToBlacklistNumber
            ), modifier
        )
    }
}