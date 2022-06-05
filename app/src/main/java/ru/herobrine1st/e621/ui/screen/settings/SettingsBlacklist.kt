package ru.herobrine1st.e621.ui.screen.settings

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
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.component.BASE_WIDTH
import ru.herobrine1st.e621.ui.dialog.StopThereAreUnsavedChangesDialog
import ru.herobrine1st.e621.ui.dialog.TextInputDialog
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor
import ru.herobrine1st.e621.util.StatefulBlacklistEntry

@Composable
fun SettingsBlacklistFloatingActionButton(applicationViewModel: ApplicationViewModel) {
    var openDialog by rememberSaveable { mutableStateOf(false) }

    if (openDialog) TextInputDialog(
        title = stringResource(R.string.add_entry_to_blacklist),
        submitButtonText = stringResource(R.string.apply),
        textFieldLabel = stringResource(R.string.tag_combination),
        onClose = { openDialog = false },
        onSubmit = {
            if (it.isBlank()) return@TextInputDialog
            applicationViewModel.addBlacklistEntry(it)
        }
    )

    FloatingActionButton(onClick = {
        openDialog = true
    }) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
    }
}

@Composable
fun blacklistHasChanges(applicationViewModel: ApplicationViewModel) = remember { derivedStateOf { applicationViewModel.blacklistDoNotUseAsFilter.any { it.isChanged } } }

@Composable
fun SettingsBlacklistAppBarActions(applicationViewModel: ApplicationViewModel) {
    val hasChanges by blacklistHasChanges(applicationViewModel)
    val coroutineScope = rememberCoroutineScope()
    if (applicationViewModel.blacklistUpdating || applicationViewModel.blacklistLoading) {
        CircularProgressIndicator(color = ActionBarIconColor)
    } else if (hasChanges) {
        IconButton(onClick = {
            coroutineScope.launch {
                applicationViewModel.applyBlacklistChanges()
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
fun SettingsBlacklist(applicationViewModel: ApplicationViewModel, onExit: () -> Unit) {
    var editQueryEntry by remember { mutableStateOf<StatefulBlacklistEntry?>(null) }
    if (editQueryEntry != null) {
        val entry = editQueryEntry ?: throw NullPointerException()
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

    val hasChanges by blacklistHasChanges(applicationViewModel)
    var openExitDialog by remember { mutableStateOf(false) }

    if (openExitDialog) StopThereAreUnsavedChangesDialog(onClose = { openExitDialog = false }) {
        applicationViewModel.resetBlacklistChanges()
        onExit()
    }

    BackHandler(enabled = hasChanges) {
        openExitDialog = true
    }

    LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
        itemsIndexed(applicationViewModel.blacklistDoNotUseAsFilter) { i, entry ->
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
                            onClick = { applicationViewModel.resetBlacklistEntry(entry) },
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
                            else applicationViewModel.deleteBlacklistEntry(entry)
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
            if (i < applicationViewModel.blacklistDoNotUseAsFilter.size - 1)
                Divider()
        }
    }
}