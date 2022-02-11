package ru.herobrine1st.e621.ui.dialog

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.R

@Composable
fun BlacklistTogglesDialog(
    applicationViewModel: ApplicationViewModel,
    onClose: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val selection = remember(
        applicationViewModel.blacklistDoNotUseAsFilter.size,
        applicationViewModel.blacklistDoNotUseAsFilter.filter { it.enabled }.size
    ) {
        applicationViewModel.blacklistDoNotUseAsFilter.map { it to it.enabled }.toMutableStateList()
    }
    var updating by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.blacklist),
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (selection.isEmpty())
                    if (applicationViewModel.blacklistLoading) CircularProgressIndicator()
                    else Text(stringResource(R.string.dialog_blacklist_empty))
                else LazyColumn(modifier = Modifier.heightIn(max = screenHeight * 0.4f)) {
                    item {
                        val checked = selection.all { it.second }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.toggleable(
                                checked,
                                remember { MutableInteractionSource() },
                                null,
                                onValueChange = {
                                    selection.replaceAll { it }
                                }
                            )
                        ) {
                            Text(
                                stringResource(R.string.selection_all),
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { selection.replaceAll { it.first to !checked } },
                                enabled = !updating,
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary)
                            )
                        }
                    }
                    itemsIndexed(selection) { i, entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.toggleable(
                                entry.second,
                                remember { MutableInteractionSource() },
                                null,
                                onValueChange = {
                                    selection[i] = entry.first to it
                                }
                            )
                        ) {
                            Text(entry.first.query, modifier = Modifier.weight(1f))
                            if (entry.first.enabled != entry.second) { // Reset
                                IconButton(
                                    enabled = !updating,
                                    onClick = { selection[i] = entry.first to entry.first.enabled },
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
                                checked = entry.second,
                                onCheckedChange = { selection[i] = entry.first to !entry.second },
                                enabled = !updating,
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (updating) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    TextButton(
                        onClick = {
                            onClose()
                        },
                        enabled = !updating
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(
                        enabled = !updating,
                        onClick = {
                            coroutineScope.launch {
                                updating = true
                                applicationViewModel.updateBlacklistSelection(selection)
                                onClose()
                                updating = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                }
            }
        }
    }
}