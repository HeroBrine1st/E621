package ru.herobrine1st.e621.ui.dialog

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.util.StatefulBlacklistEntry

@Composable
fun BlacklistTogglesDialog(
    blacklistEntries: SnapshotStateList<StatefulBlacklistEntry>,
    isBlacklistUpdating: Boolean,
    isBlacklistLoading: Boolean,
    isBlacklistEnabled: Boolean,
    toggleBlacklist: (Boolean) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit
) {
    if (isBlacklistLoading) {
        ActionDialog(
            title = stringResource(R.string.blacklist),
            actions = {
                DialogActions(enabled = false)
            },
            onDismissRequest = onClose
        ) {
            CircularProgressIndicator()
        }
        return
    }

    ActionDialog(
        title = stringResource(R.string.blacklist),
        actions = {
            if (isBlacklistUpdating) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            DialogActions(!isBlacklistUpdating, onApply = {
                onApply()
                onClose()
            }, onCancel = {
                onCancel()
                onClose()
            })
        }, onDismissRequest = {
            onCancel()
            onClose()
        }
    ) {
        if (blacklistEntries.isEmpty()) Text(stringResource(R.string.dialog_blacklist_empty))
        else BlacklistTogglesDialogContent(
            blacklistEntries,
            isBlacklistUpdating,
            isBlacklistEnabled,
            toggleBlacklist
        )
    }
}

@Composable
private fun BlacklistTogglesDialogContent(
    blacklistEntries: SnapshotStateList<StatefulBlacklistEntry>,
    isBlacklistUpdating: Boolean,
    isBlacklistEnabled: Boolean,
    toggleBlacklist: (Boolean) -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    LazyColumn(modifier = Modifier.heightIn(max = screenHeight * 0.4f)) {
        item {
            val onChange: (Boolean) -> Unit = {
                toggleBlacklist(it)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(
                    isBlacklistEnabled,
                    remember { MutableInteractionSource() },
                    null,
                    onValueChange = onChange
                )
            ) {
                Text(
                    stringResource(if (isBlacklistEnabled) R.string.blacklist_enabled else R.string.blacklist_disabled),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isBlacklistEnabled,
                    onCheckedChange = onChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary,
                        uncheckedThumbColor = MaterialTheme.colors.onSurface
                    )
                )
            }
            Divider()
        }
        item {
            val isAllEnabled = blacklistEntries.all { it.enabled }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(
                    isAllEnabled,
                    remember { MutableInteractionSource() },
                    null,
                    enabled = !isBlacklistUpdating,
                    onValueChange = {
                        // TODO wtf
                        blacklistEntries.replaceAll { it }
                    }
                )
            ) {
                Text(
                    stringResource(R.string.selection_all),
                    modifier = Modifier.weight(1f),
                    color = if (isBlacklistUpdating) Color.Gray else Color.Unspecified
                )
                Checkbox(
                    checked = isAllEnabled,
                    onCheckedChange = { blacklistEntries.forEach { it.enabled = !isAllEnabled } },
                    enabled = !isBlacklistUpdating,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary)
                )
            }
            if (blacklistEntries.isNotEmpty())
                Divider()
        }
        itemsIndexed(blacklistEntries) { i, entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(
                    entry.enabled,
                    remember { MutableInteractionSource() },
                    null,
                    onValueChange = {
                        entry.enabled = it
                    }
                )
            ) {
                Text(
                    entry.query,
                    modifier = Modifier.weight(1f),
                    color = if (isBlacklistUpdating) Color.Gray else Color.Unspecified
                )
                if (entry.isToggled) {
                    ResetButton(isBlacklistUpdating, entry)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Checkbox(
                    checked = entry.enabled,
                    onCheckedChange = { entry.enabled = it },
                    enabled = !isBlacklistUpdating,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary)
                )
            }
            if (i < blacklistEntries.size - 1)
                Divider()
        }
    }
}


// Extracted because of ClassCastException (idk what tf has happened but it works like this or without if statement)
@Composable
private fun ResetButton(isBlacklistUpdating: Boolean, entry: StatefulBlacklistEntry) {
    IconButton(
        enabled = !isBlacklistUpdating,
        onClick = {
            entry.enabled = !entry.enabled
        },
        modifier = Modifier.size(24.dp)
    ) {
        Icon(
            Icons.Outlined.Undo,
            contentDescription = stringResource(R.string.cancel)
        )
    }
}

@Composable
private fun DialogActions(
    enabled: Boolean,
    onApply: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    TextButton(
        onClick = { onCancel() },
        enabled = enabled
    ) {
        Text(stringResource(R.string.cancel))
    }
    TextButton(
        enabled = enabled,
        onClick = onApply
    ) {
        Text(stringResource(R.string.apply))
    }
}