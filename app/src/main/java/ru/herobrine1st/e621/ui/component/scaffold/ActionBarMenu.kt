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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R

@Composable
fun MenuAction(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Icon(icon, null, modifier = Modifier.padding(end = 8.dp))
        Text(text)
    }
}

/**
 * @param onNavigateToSettings called when user click "Settings". Should prohibit multiple Settings configurations in backstack.
 */
@Composable
fun ActionBarMenu(
    onNavigateToSettings: () -> Unit,
    onOpenBlacklistDialog: () -> Unit
) {
    var openMenu by remember { mutableStateOf(false) }

    androidx.compose.material3.IconButton(onClick = { openMenu = !openMenu }) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.appbar_morevert)
        )
    }

    DropdownMenu(
        expanded = openMenu,
        onDismissRequest = { openMenu = false }
    ) {
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