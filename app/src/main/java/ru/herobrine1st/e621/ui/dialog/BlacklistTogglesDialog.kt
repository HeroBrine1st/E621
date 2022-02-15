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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.preference.BLACKLIST_ENABLED
import ru.herobrine1st.e621.preference.getPreference
import ru.herobrine1st.e621.preference.setPreference

@Composable
fun BlacklistTogglesDialog(
    applicationViewModel: ApplicationViewModel,
    onClose: () -> Unit,
) {
    val blacklist = applicationViewModel.blacklistDoNotUseAsFilter
    val updating = applicationViewModel.blacklistUpdating

    if (applicationViewModel.blacklistLoading) {
        ActionDialog(
            title = stringResource(R.string.blacklist),
            actions = {
                BlacklistTogglesDialogActions(false, applicationViewModel, false)
            },
            onDismissRequest = onClose
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val closeCancel = {
        blacklist.forEach { if (it.isToggled()) /*then*/ it.enabled = !it.enabled }
        onClose()
    }

    ActionDialog(
        title = stringResource(R.string.blacklist),
        actions = {
            BlacklistTogglesDialogActions(updating, applicationViewModel) {
                if (it) closeCancel() else onClose()
            }
        }, onDismissRequest = closeCancel
    ) {
        if (blacklist.isEmpty()) Text(stringResource(R.string.dialog_blacklist_empty))
        else BlacklistTogglesDialogContent(applicationViewModel)
    }
}

@Composable
private fun BlacklistTogglesDialogContent(applicationViewModel: ApplicationViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val blacklist = applicationViewModel.blacklistDoNotUseAsFilter
    val updating = applicationViewModel.blacklistUpdating

    LazyColumn(modifier = Modifier.heightIn(max = screenHeight * 0.4f)) {
        item {
            val context = LocalContext.current
            val checked = context.getPreference(BLACKLIST_ENABLED, defaultValue = true)
            val onChange: (Boolean) -> Unit = {
                coroutineScope.launch {
                    context.setPreference(BLACKLIST_ENABLED, it)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(
                    checked,
                    remember { MutableInteractionSource() },
                    null,
                    onValueChange = onChange
                )
            ) {
                Text(
                    stringResource(if (checked) R.string.blacklist_enabled else R.string.blacklist_disabled),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = checked,
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
            val checked = blacklist.all { it.enabled }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(
                    checked,
                    remember { MutableInteractionSource() },
                    null,
                    enabled = !updating,
                    onValueChange = {
                        blacklist.replaceAll { it }
                    }
                )
            ) {
                Text(
                    stringResource(R.string.selection_all),
                    modifier = Modifier.weight(1f),
                    color = if (updating) Color.Gray else Color.Unspecified
                )
                Checkbox(
                    checked = checked,
                    onCheckedChange = { blacklist.forEach { it.enabled = !checked } },
                    enabled = !updating,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary)
                )
            }
            if (blacklist.isNotEmpty())
                Divider()
        }
        itemsIndexed(blacklist) { i, entry ->
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
                    color = if (updating) Color.Gray else Color.Unspecified
                )
                if (entry.isToggled()) { // Reset
                    IconButton(
                        enabled = !updating,
                        onClick = { entry.enabled = !entry.enabled },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Undo,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Checkbox(
                    checked = entry.enabled,
                    onCheckedChange = { entry.enabled = it },
                    enabled = !updating,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary)
                )
            }
            if (i < blacklist.size - 1)
                Divider()
        }
    }
}

@Composable
private fun BlacklistTogglesDialogActions(
    updating: Boolean,
    applicationViewModel: ApplicationViewModel,
    enabled: Boolean = !updating,
    onClose: (cancel: Boolean) -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    if (updating) CircularProgressIndicator(modifier = Modifier.size(24.dp))
    TextButton(
        onClick = { onClose(true) },
        enabled = enabled
    ) {
        Text(stringResource(R.string.cancel))
    }
    TextButton(
        enabled = enabled,
        onClick = {
            coroutineScope.launch {
                applicationViewModel.applyBlacklistChanges()
                onClose(false)
            }
        }
    ) {
        Text(stringResource(R.string.apply))
    }
}