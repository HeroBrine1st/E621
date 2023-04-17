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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.settings.EditedBlacklistEntry
import ru.herobrine1st.e621.navigation.component.settings.SettingsBlacklistComponent
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffold
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffoldState
import ru.herobrine1st.e621.ui.dialog.StopThereAreUnsavedChangesDialog
import ru.herobrine1st.e621.ui.dialog.TextInputDialog
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor


@Composable
fun blacklistHasChanges(entries: List<EditedBlacklistEntry>?) =
    remember(entries) { entries?.any { it.isChanged } ?: false }

@Composable
fun SettingsBlacklist(
    mainScaffoldState: MainScaffoldState,
    component: SettingsBlacklistComponent, exit: () -> Unit
) {
    val entries = component.entriesFlow.collectAsState().value
    var editQueryEntry by remember { mutableStateOf<EditedBlacklistEntry?>(null) }
    val hasChanges = blacklistHasChanges(entries)
    var openExitDialog by remember { mutableStateOf(false) }
    var openAddDialog by rememberSaveable { mutableStateOf(false) }

    MainScaffold(
        state = mainScaffoldState,
        title = { Text(stringResource(R.string.blacklist)) },
        appBarActions = {
            if (component.isUpdating || entries == null) {
                CircularProgressIndicator(color = ActionBarIconColor)
            } else if (hasChanges) {
                IconButton(onClick = { component.applyChanges() }) {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = stringResource(R.string.search),
                        tint = ActionBarIconColor
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                openAddDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }

        }
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (entries != null) itemsIndexed(entries) { i, entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = BASE_PADDING_HORIZONTAL)
                        .fillMaxWidth()
                ) {
                    if (entry.isPendingInsertion) {
                        key("New item indicator") {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.new_item)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                    key("Query string") {
                        Text(
                            entry.query, modifier = Modifier.weight(1f), color = when {
                                entry.isPendingInsertion -> Color.Unspecified
                                entry.isPendingUpdate -> Color.Unspecified
                                entry.isPendingDeletion -> Color.Red
                                else -> Color.Unspecified
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (entry.isChanged) {
                        key("Undo button") {
                            IconButton(
                                onClick = { component.resetEntry(entry) },
                            ) {
                                Icon(
                                    Icons.Outlined.Undo,
                                    contentDescription = stringResource(R.string.cancel)
                                )
                            }
                        }
                    }
                    key("Delete button") {
                        IconButton(
                            onClick = {
                                component.markEntryAsDeleted(entry, !entry.isPendingDeletion)
                            }
                        ) {
                            Icon(
                                if (entry.isPendingDeletion) Icons.Default.Add else Icons.Default.Remove,
                                contentDescription = stringResource(R.string.remove)
                            )
                        }
                    }

                    key("Edit button") {
                        IconButton(
                            onClick = { editQueryEntry = entry }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit)
                            )
                        }
                    }

                    key("Enable/disable checkbox") {
                        Checkbox(checked = entry.enabled, onCheckedChange = {
                            component.editEntry(entry, enabled = it)
                        })
                    }
                }
                if (i < entries.size - 1)
                    Divider()
            }
        }
    }

    BackHandler(enabled = hasChanges) {
        openExitDialog = true
    }

    if (openExitDialog) StopThereAreUnsavedChangesDialog(onClose = { openExitDialog = false }) {
        component.resetChanges()
        exit()
    }

    if (editQueryEntry != null) {
        val entry = editQueryEntry!!
        TextInputDialog(
            title = stringResource(R.string.edit_blacklist_entry),
            submitButtonText = stringResource(R.string.apply),
            textFieldLabel = stringResource(R.string.tag_combination),
            initialText = entry.query,
            onClose = { editQueryEntry = null },
            onSubmit = {
                component.editEntry(
                    entry, query = it
                )
            }
        )
    }

    if (openAddDialog) TextInputDialog(
        title = stringResource(R.string.add_entry_to_blacklist),
        submitButtonText = stringResource(R.string.apply),
        textFieldLabel = stringResource(R.string.tag_combination),
        onClose = { openAddDialog = false },
        onSubmit = {
            if (it.isBlank()) return@TextInputDialog
            component.appendEntry(it)
        }
    )
}

