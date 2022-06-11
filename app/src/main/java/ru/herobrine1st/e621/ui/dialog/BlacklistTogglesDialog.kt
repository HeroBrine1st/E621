package ru.herobrine1st.e621.ui.dialog

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.util.BlacklistCache
import ru.herobrine1st.e621.util.StatefulBlacklistEntry
import javax.inject.Inject

@Composable
fun BlacklistTogglesDialog(
    isBlacklistEnabled: Boolean,
    toggleBlacklist: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val viewModel: BlacklistTogglesDialogViewModel = hiltViewModel()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    var isBlacklistUpdating by remember { mutableStateOf(false) }
    val isBlacklistLoading = viewModel.isBlacklistLoading
    val blacklistEntries: List<StatefulBlacklistEntry> = viewModel.entries

    val onCancel = {
        blacklistEntries.forEach { it.resetChanges() }
        onClose()
    }

    ActionDialog(
        title = stringResource(R.string.blacklist),
        actions = {
            if (isBlacklistUpdating) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            DialogActions(!isBlacklistUpdating, onApply = {
                isBlacklistUpdating = true
                viewModel.viewModelScope.launch {
                    viewModel.applyChanges()
                    isBlacklistUpdating = false
                    onClose()
                }
            }, onCancel = {
                onCancel()
            })
        }, onDismissRequest = {
            onCancel()
        }
    ) {
        if (blacklistEntries.isEmpty() && !isBlacklistLoading) Text(stringResource(R.string.dialog_blacklist_empty))
        else LazyColumn(
            modifier = Modifier.height(screenHeight * 0.4f),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.toggleable(
                        isBlacklistEnabled,
                        remember { MutableInteractionSource() },
                        null,
                        onValueChange = toggleBlacklist
                    )
                ) {
                    Text(
                        stringResource(if (isBlacklistEnabled) R.string.blacklist_enabled else R.string.blacklist_disabled),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isBlacklistEnabled,
                        onCheckedChange = toggleBlacklist,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colors.primary,
                            uncheckedThumbColor = MaterialTheme.colors.onSurface
                        )
                    )
                }
                Divider()
            }
            if (isBlacklistLoading) {
                items(50) {
                    BlacklistEntryLine(
                        value = true,
                        onValueChange = {},
                        text = "",
                        isBlacklistUpdating = isBlacklistUpdating,
                        placeholder = true
                    )
                }
                return@LazyColumn
            }
            item {
                val isAllEnabled = blacklistEntries.all { it.enabled }
                BlacklistEntryLine(
                    value = isAllEnabled,
                    onValueChange = { blacklistEntries.forEach { it.enabled = !isAllEnabled } },
                    text = stringResource(R.string.selection_all),
                    isBlacklistUpdating = isBlacklistUpdating
                )
                if (blacklistEntries.isNotEmpty())
                    Divider()
            }
            itemsIndexed(blacklistEntries) { i, entry ->
                BlacklistEntryLine(
                    value = entry.enabled,
                    onValueChange = { entry.enabled = it },
                    text = entry.query,
                    isBlacklistUpdating = isBlacklistUpdating,
                    showResetButton = entry.isChanged,
                    onReset = { entry.resetChanges() }
                )
                if (i < blacklistEntries.size - 1)
                    Divider()
            }
        }
    }
}

@Composable
fun BlacklistEntryLine(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    text: String,
    isBlacklistUpdating: Boolean,
    showResetButton: Boolean = false,
    onReset: () -> Unit = {},
    placeholder: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.toggleable(
            value,
            remember { MutableInteractionSource() },
            null,
            onValueChange = onValueChange
        )
    ) {
        Text(
            text,
            modifier = Modifier
                .weight(1f)
                .placeholder(placeholder, highlight = PlaceholderHighlight.shimmer()),
            color = if (isBlacklistUpdating) Color.Gray else Color.Unspecified
        )
        if (showResetButton) {
            IconButton(
                enabled = !isBlacklistUpdating,
                onClick = onReset,
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
            checked = value,
            onCheckedChange = onValueChange,
            enabled = !placeholder && !isBlacklistUpdating,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary),
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

@HiltViewModel
class BlacklistTogglesDialogViewModel @Inject constructor(
    private val cache: BlacklistCache
) : ViewModel() {
    val entries = cache.entries

    val isBlacklistLoading get() = cache.isLoading

    suspend fun applyChanges() {
        cache.applyChanges()
    }
}