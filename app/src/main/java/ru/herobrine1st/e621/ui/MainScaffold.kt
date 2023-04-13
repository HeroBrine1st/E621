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

package ru.herobrine1st.e621.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.preference.updatePreferences
import ru.herobrine1st.e621.ui.dialog.BlacklistTogglesDialog
import ru.herobrine1st.e621.ui.screen.Screen

@Composable
fun MainScaffold(
    navController: NavHostController,
    scaffoldState: ScaffoldState,
    screen: Screen,
    content: @Composable () -> Unit
) {
    val preferences = LocalPreferences.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
rememberScaffoldState()
    var openBlacklistDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(screen.title))
                },
                backgroundColor = MaterialTheme.colors.primarySurface,
                elevation = 12.dp,
                actions = {
                    // TODO expose as parameter
                    screen.appBarActions(this, navController)

                    ActionBarMenu(navController) {
                        openBlacklistDialog = true
                    }
                }
            )
        },
        scaffoldState = scaffoldState,
        floatingActionButton = {
            screen.floatingActionButton()
        }
    ) {
        Surface(
            color = MaterialTheme.colors.background,
            modifier = Modifier.padding(it),
            content = content
        )
    }

    if (openBlacklistDialog)
        BlacklistTogglesDialog(
            isBlacklistEnabled = preferences.blacklistEnabled,
            toggleBlacklist = { enabled: Boolean ->
                coroutineScope.launch {
                    context.updatePreferences { blacklistEnabled = enabled }
                }
            },
            onClose = { openBlacklistDialog = false })
}