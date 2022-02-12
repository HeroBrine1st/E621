package ru.herobrine1st.e621.ui.dialog

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val blacklist = applicationViewModel.blacklistDoNotUseAsFilter
    val updating = applicationViewModel.blacklistUpdating

    //region "Fucking dialog does not change its fucking size" workaround
    var fuckingDialogDoesNotChangeItsFuckingSizeWorkaround by remember {
        mutableStateOf(
            applicationViewModel.blacklistLoading
        )
    }
    if (applicationViewModel.blacklistLoading != fuckingDialogDoesNotChangeItsFuckingSizeWorkaround) {
        // If blacklist is just loaded, decompose this frame and compose again on next composition,
        // causing dialog to decompose & compose with valid size
        @Suppress("UNUSED_VALUE")
        fuckingDialogDoesNotChangeItsFuckingSizeWorkaround = applicationViewModel.blacklistLoading
        return
    }
    //endregion

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
                if (blacklist.isEmpty())
                    if (applicationViewModel.blacklistLoading) CircularProgressIndicator()
                    else Text(stringResource(R.string.dialog_blacklist_empty))
                else LazyColumn(modifier = Modifier.heightIn(max = screenHeight * 0.4f)) {
                    item {
                        val checked = blacklist.all { it.enabled }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.toggleable(
                                checked,
                                remember { MutableInteractionSource() },
                                null,
                                onValueChange = {
                                    blacklist.replaceAll { it }
                                }
                            )
                        ) {
                            Text(
                                stringResource(R.string.selection_all),
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { blacklist.forEach { it.enabled = !checked } },
                                enabled = !updating,
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.secondary)
                            )
                        }
                    }
                    items(blacklist) { entry ->
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
                            Text(entry.query, modifier = Modifier.weight(1f))
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
                            blacklist.forEach { if (it.isToggled()) /*then*/ it.resetChanges() }
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
                                applicationViewModel.applyBlacklistChanges()
                                onClose()
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