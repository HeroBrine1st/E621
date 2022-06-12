package ru.herobrine1st.e621.ui.screen.settings

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.ui.component.BASE_WIDTH
import ru.herobrine1st.e621.ui.dialog.StopThereAreUnsavedChangesDialog
import ru.herobrine1st.e621.ui.dialog.TextInputDialog
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor
import ru.herobrine1st.e621.util.StatefulBlacklistEntry
import ru.herobrine1st.e621.util.asStateful
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
fun blacklistHasChanges(entries: List<StatefulBlacklistEntry>) =
    remember { derivedStateOf { entries.any { it.isChanged } } }


@Composable
fun SettingsBlacklistAppBarActions() {
    val viewModel: SettingsBlacklistViewModel = hiltViewModel()

    val hasChanges by blacklistHasChanges(viewModel.entries)
    val coroutineScope = rememberCoroutineScope()
    if (viewModel.isUpdating || viewModel.isLoading) {
        CircularProgressIndicator(color = ActionBarIconColor)
    } else if (hasChanges) {
        IconButton(onClick = {
            coroutineScope.launch {
                viewModel.applyChanges()
            }
        }) {
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

    var editQueryEntry by remember { mutableStateOf<StatefulBlacklistEntry?>(null) }
    val hasChanges by blacklistHasChanges(viewModel.entries)
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
                entry.query = it
            }
        )
    }

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(viewModel.entries) { i, entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(BASE_WIDTH)
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
                            if (entry.isPendingDeletion) entry.markAsDeleted(false)
                            else viewModel.markEntryAsDeleted(entry)
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
                    Checkbox(checked = entry.enabled, onCheckedChange = { entry.enabled = it })
                }
            }
            if (i < viewModel.entries.size - 1)
                Divider()
        }
    }
}


@HiltViewModel
class SettingsBlacklistViewModel @Inject constructor(
    private val snackbar: SnackbarAdapter,
    private val blacklistRepository: BlacklistRepository
) : ViewModel() {
    private val _entries = mutableStateListOf<StatefulBlacklistEntry>()

    val entries: List<StatefulBlacklistEntry> = _entries

    init {
        viewModelScope.launch {
            // State of this screen should be shared between three composables (in the same
            // navgraph but one under NavHost and two under Scaffold) so that ViewModel is the
            // most fitting pattern
            //
            // Looks like this ViewModel is cleared when user exits the settings screen
            // so that there's no resource consuming
            //
            // Another solution: extract this block to suspend function and call from composable's
            // coroutine scope and this solution is obviously dirty workaround
            //
            // ..and another: somehow emit SnapshotStateList, reuse it across all changes in the
            // database and use stateIn with SharingStarted.WhileSubscribed() on this flow.
            //
            // Best solution: use immutable entries with "our" state and database state and somehow
            // emit either our change (that isn't committed) or database change.
            // TODO use SharedFlow (or even StateFlow) to do ^
            blacklistRepository.getEntriesFlow().collect { list ->
                // Just reset all changes, couldn't write good algorithm
                // TODO somehow save changes if entry is unchanged
                val new = list.map { it.asStateful() }
                _entries.clear()
                _entries.addAll(new)
                isLoading = false
            }
        }
    }

    var isLoading by mutableStateOf(true)
        private set
    var isUpdating by mutableStateOf(false)
        private set

    suspend fun applyChanges() {
        isUpdating = true
        var wasError = false
        blacklistRepository.withTransaction {
            val iterator = _entries.listIterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                try {
                    when {
                        entry.isPendingInsertion -> blacklistRepository.insertEntry(entry.toEntry())
                        entry.isPendingUpdate -> blacklistRepository.updateEntry(entry.toEntry())
                        entry.isPendingDeletion -> blacklistRepository.deleteEntryById(entry.id)
                    }
                } catch (t: Throwable) {
                    if (entry.isPendingInsertion) iterator.remove() else entry.resetChanges()
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
                }
            }
            isUpdating = false
        }
    }

    fun appendEntry(query: String) = _entries.add(StatefulBlacklistEntry.create(query))

    fun resetEntry(entry: StatefulBlacklistEntry) {
        if (entry.isPendingInsertion) _entries.remove(entry)
        else entry.resetChanges()
    }

    fun markEntryAsDeleted(entry: StatefulBlacklistEntry, deleted: Boolean = true) {
        if (entry.isPendingInsertion) _entries.remove(entry)
        else entry.markAsDeleted(deleted)
    }

    fun resetChanges() {
        _entries.removeIf { it.isPendingInsertion }
        _entries.forEach { it.resetChanges() }
    }

    companion object {
        const val TAG = "SettingsBlacklistViewModel"
    }
}