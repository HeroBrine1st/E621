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

package ru.herobrine1st.e621.ui.screen.settings

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.settings.SettingsBlacklistComponent
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffold
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffoldState
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor


@Composable
fun SettingsBlacklist(
    mainScaffoldState: MainScaffoldState,
    component: SettingsBlacklistComponent
) {
    val entries by component.entriesFlow.collectAsState()

    MainScaffold(
        state = mainScaffoldState,
        title = { Text(stringResource(R.string.blacklist)) },
        appBarActions = {
            if (component.isUpdating || entries == null) {
                CircularProgressIndicator(color = ActionBarIconColor)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                component.createNewEntry()
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }

        }
    ) {
        Crossfade(entries) { entries ->
            when (entries) {
                null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(
                        entries,
                        key = { _, entry ->
                            entry.id
                        }
                    ) { i, entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(horizontal = BASE_PADDING_HORIZONTAL)
                                .fillMaxWidth()
                        ) {
                            key("Query string") {
                                Text(
                                    entry.query, modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            key("Delete button") {
                                var enabled by remember { mutableStateOf(true) }
                                IconButton(
                                    onClick = {
                                        enabled = false
                                        component.deleteEntry(entry) {
                                            // TODO fade out
                                        }
                                    },
                                    enabled = enabled
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = stringResource(R.string.remove)
                                    )
                                }
                            }
                            key("Edit button") {
                                IconButton(
                                    onClick = { component.editEntry(entry) }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.edit)
                                    )
                                }
                            }
                            key("Enable/disable checkbox") {
                                var enabled by remember { mutableStateOf(true) }
                                // FIXME all checkboxes are disabled
                                //       adding id to key does not help
                                //       tested on emulator with API 33
                                Checkbox(
                                    checked = entry.enabled,
                                    onCheckedChange = {
                                        enabled = false
                                        component.toggleEntry(entry) {
                                            enabled = true
                                        }
                                    },
                                    enabled = enabled
                                )
                            }
                        }
                        if (i < entries.size - 1)
                            Divider(Modifier.padding(horizontal = 8.dp))
                    }
                }
            }
        }
    }
}

