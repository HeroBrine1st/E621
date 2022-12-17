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

package ru.herobrine1st.e621.ui.screen.settings

import android.util.Log
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.dialog.StopThereAreUnsavedChangesDialog
import ru.herobrine1st.e621.ui.dialog.TextInputDialog
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor
import javax.inject.Inject

@Composable
fun SettingsBlacklistFloatingActionButton() {
    val viewModel: SettingsBlacklistViewModel = hiltViewModel()

    var openDialog by rememberSaveable { mutableStateOf(false) }

    if (openDialog) TextInputDialog(
        title = stringResource(R.string.add_entry_to_blacklist),
        submitButtonText = stringResource(R.string.apply),
        textFieldLabel = stringResource(R.string.tag_combination),
        onClose = { openDialog = false },
        onSubmit = {
            if (it.isBlank()) return@TextInputDialog
            viewModel.appendEntry(it)
        }
    )

    FloatingActionButton(onClick = {
        openDialog = true
    }) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
    }
}

@Composable
fun blacklistHasChanges(entries: List<EditedBlacklistEntry>?) =
    remember(entries) { entries?.any { it.isChanged } ?: false }


@Composable
fun SettingsBlacklistAppBarActions() {
    val viewModel: SettingsBlacklistViewModel = hiltViewModel()

    val entries by viewModel.entriesFlow.collectAsState()
    val hasChanges = blacklistHasChanges(entries)

    if (viewModel.isUpdating || entries == null) {
        CircularProgressIndicator(color = ActionBarIconColor)
    } else if (hasChanges) {
        IconButton(onClick = { viewModel.applyChanges() }) {
            Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = stringResource(R.string.search),
                tint = ActionBarIconColor
            )
        }
    }
}

@Composable
fun SettingsBlacklist(exit: () -> Unit) {
    val viewModel: SettingsBlacklistViewModel = hiltViewModel()
    val entries = viewModel.entriesFlow.collectAsState().value
    var editQueryEntry by remember { mutableStateOf<EditedBlacklistEntry?>(null) }
    val hasChanges = blacklistHasChanges(entries)
    var openExitDialog by remember { mutableStateOf(false) }



    BackHandler(enabled = hasChanges) {
        openExitDialog = true
    }

    if (openExitDialog) StopThereAreUnsavedChangesDialog(onClose = { openExitDialog = false }) {
        viewModel.resetChanges()
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
                viewModel.editEntry(
                    entry, query = it
                )
            }
        )
    }

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
                            onClick = { viewModel.resetEntry(entry) },
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
                            viewModel.markEntryAsDeleted(entry, !entry.isPendingDeletion)
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
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }
                }

                key("Enable/disable checkbox") {
                    Checkbox(checked = entry.enabled, onCheckedChange = {
                        viewModel.editEntry(entry, enabled = it)
                    })
                }
            }
            if (i < entries.size - 1)
                Divider()
        }
    }
}


@HiltViewModel
class SettingsBlacklistViewModel @Inject constructor(
    private val snackbar: SnackbarAdapter,
    private val blacklistRepository: BlacklistRepository
) : ViewModel() {
    private val _entriesFlow = MutableStateFlow<List<EditedBlacklistEntry>?>(null)
    val entriesFlow = _entriesFlow.asStateFlow()

    init {
        viewModelScope.launch {
            _entriesFlow.emitAll(blacklistRepository.getEntriesFlow().map { list ->
                list.map {
                    EditedBlacklistEntry.from(it)
                }
            })
        }
    }

    var isUpdating by mutableStateOf(false)
        private set

    fun applyChanges() {
        isUpdating = true
        var wasError = false
        val oldValue = _entriesFlow.value
        viewModelScope.launch {
            blacklistRepository.withTransaction {
                _entriesFlow.compareAndSet(oldValue, _entriesFlow.value?.mapNotNull { entry ->
                    try {
                        when {
                            entry.isPendingInsertion -> entry.copy(
                                backingEntry = BlacklistEntry(
                                    query = entry.query,
                                    enabled = entry.enabled,
                                    id = blacklistRepository.insertEntry(entry.toEntry())
                                )
                            )
                            entry.isPendingUpdate -> {
                                blacklistRepository.updateEntry(entry.toEntry())
                                entry.copy(
                                    backingEntry = entry.backingEntry!!.copy(
                                        query = entry.query,
                                        enabled = entry.enabled
                                    )
                                )
                            }
                            entry.isPendingDeletion -> {
                                blacklistRepository.deleteEntryById(entry.id!!)
                                null
                            }
                            else -> entry
                        }
                    } catch (t: Throwable) {
                        Log.e(
                            TAG,
                            "Unknown error occurred while trying to update/insert/delete blacklist entry",
                            t
                        )
                        if (!wasError) {
                            snackbar.enqueueMessage( // Likely database in release and something else in tests
                                R.string.database_error_updating_blacklist,
                                SnackbarDuration.Long,
                                entry.query
                            )
                            wasError = true
                        }
                        entry.reset()
                    }
                })
            }
            isUpdating = false
        }
    }

    fun appendEntry(query: String) {
        _entriesFlow.value = _entriesFlow.value?.plus(EditedBlacklistEntry(query))
    }

    fun resetEntry(entry: EditedBlacklistEntry) {
        if (entry.isPendingInsertion) {
            _entriesFlow.value = _entriesFlow.value?.minus(entry)
        } else {
            _entriesFlow.value = _entriesFlow.value?.mapNotNull {
                if (it == entry) entry.reset() else it
            }
        }
    }

    fun markEntryAsDeleted(entry: EditedBlacklistEntry, deleted: Boolean = true) {
        if (entry.isPendingInsertion) {
            _entriesFlow.value = _entriesFlow.value?.minus(entry)
        } else {
            _entriesFlow.value = _entriesFlow.value
                ?.map {
                    if (it == entry) entry.copy(pendingDeletion = deleted) else it
                }
        }
    }

    fun resetChanges() {
        _entriesFlow.value =
            _entriesFlow.value?.mapNotNull { it.reset() }
    }

    fun editEntry(
        entry: EditedBlacklistEntry,
        query: String = entry.query,
        enabled: Boolean = entry.enabled
    ) {
        _entriesFlow.value = _entriesFlow.value?.map {
            if (it == entry) entry.copy(query = query, enabled = enabled) else it
        }
    }

    companion object {
        const val TAG = "SettingsBlacklistViewModel"
    }
}

@Immutable
data class EditedBlacklistEntry(
    val query: String,
    val enabled: Boolean = true,
    val pendingDeletion: Boolean = false,
    val backingEntry: BlacklistEntry? = null
) {
    val isPendingInsertion get() = backingEntry == null
    val isPendingUpdate get() = backingEntry != null && (query != backingEntry.query || enabled != backingEntry.enabled)
    val isPendingDeletion get() = backingEntry != null && pendingDeletion
    val isChanged get() = backingEntry == null || query != backingEntry.query || enabled != backingEntry.enabled || pendingDeletion

    val id get() = backingEntry?.id

    fun reset() = backingEntry?.let { from(it) }

    fun toEntry(): BlacklistEntry = BlacklistEntry(
        query = query,
        enabled = enabled,
        id = backingEntry?.id ?: 0
    )

    companion object {
        fun from(blacklistEntry: BlacklistEntry) = EditedBlacklistEntry(
            blacklistEntry.query,
            blacklistEntry.enabled,
            false,
            blacklistEntry
        )
    }
}