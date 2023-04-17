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

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.herobrine1st.e621.ui.ActionBarMenu

@Composable
fun MainScaffold(
    state: MainScaffoldState,
    title: @Composable () -> Unit,
    appBarActions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                actions = {
                    // TODO create API for actions
                    appBarActions()
                    ActionBarMenu(
                        onNavigateToSettings = state.goToSettings,
                        onOpenBlacklistDialog = state.openBlacklistDialog
                    )
                }
            )
        },
        scaffoldState = state.scaffoldState,
        floatingActionButton = floatingActionButton
    ) {
        Surface(
            color = MaterialTheme.colors.background,
            modifier = Modifier.padding(it),
            content = content
        )
    }
}
