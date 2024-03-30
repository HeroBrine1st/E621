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

package ru.herobrine1st.e621.ui.component.scaffold

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R

@Composable
fun MenuAction(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(text)
        },
        leadingIcon = {
            Icon(icon, null)
        },
        onClick = onClick
    )
}

/**
 * @param onNavigateToSettings called when user click "Settings". Should prohibit multiple Settings configurations in backstack.
 */
@Composable
fun ActionBarMenu(
    onNavigateToSettings: () -> Unit,
    onOpenBlacklistDialog: () -> Unit,
    additionalMenuActions: @Composable () -> Unit = {},
) {
    var openMenu by remember { mutableStateOf(false) }

    IconButton(onClick = { openMenu = !openMenu }) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.appbar_morevert)
        )
    }

    DropdownMenu(
        expanded = openMenu,
        onDismissRequest = { openMenu = false }
    ) {
        additionalMenuActions()
        MenuAction(Icons.Outlined.Block, stringResource(R.string.blacklist)) {
            openMenu = false
            onOpenBlacklistDialog()
        }
        MenuAction(Icons.Outlined.Settings, stringResource(R.string.settings)) {
            openMenu = false
            onNavigateToSettings()
        }
    }
}