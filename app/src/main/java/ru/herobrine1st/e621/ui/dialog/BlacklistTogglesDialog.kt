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

package ru.herobrine1st.e621.ui.dialog

import androidx.compose.animation.Crossfade
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
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.BlacklistTogglesDialogComponent
import ru.herobrine1st.e621.preference.LocalPreferences

@Composable
fun BlacklistTogglesDialog(
    component: BlacklistTogglesDialogComponent
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isBlacklistEnabled = LocalPreferences.current.blacklistEnabled

    var isBlacklistUpdating by remember { mutableStateOf(false) }
    val isBlacklistLoading: Boolean
    val blacklistEntries = component.entriesFlow
        .collectAsState().value.also {
            isBlacklistLoading = it == null
        } ?: emptyList()

    val onCancel = {
        blacklistEntries.forEach { it.resetChanges() }
        component.onClose()
    }

    ActionDialog(
        title = stringResource(R.string.blacklist),
        actions = {
            if (isBlacklistUpdating) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            DialogActions(!isBlacklistUpdating, onApply = {
                isBlacklistUpdating = true
                component.applyChanges {
                    isBlacklistUpdating = false
                    component.onClose()
                }
            }, onCancel = {
                onCancel()
            })
        }, onDismissRequest = {
            onCancel()
        }
    ) {
        if (!isBlacklistLoading && blacklistEntries.isEmpty())
            Text(stringResource(R.string.dialog_blacklist_empty))
        else Crossfade(isBlacklistLoading) { loading ->
            if (loading) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(screenHeight * 0.4f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else LazyColumn(
                modifier = Modifier.height(screenHeight * 0.4f),
                userScrollEnabled = !isBlacklistLoading
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.toggleable(
                            isBlacklistEnabled,
                            remember { MutableInteractionSource() },
                            null,
                            onValueChange = component::toggleBlacklist
                        )
                    ) {
                        Text(
                            stringResource(if (isBlacklistEnabled) R.string.blacklist_enabled else R.string.blacklist_disabled),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isBlacklistEnabled,
                            onCheckedChange = component::toggleBlacklist,
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