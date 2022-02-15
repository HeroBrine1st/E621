package ru.herobrine1st.e621.ui.screen

import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.Order
import ru.herobrine1st.e621.api.Rating
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.OutlinedChip
import ru.herobrine1st.e621.ui.dialog.ActionDialog
import ru.herobrine1st.e621.util.SearchOptions


@Composable
fun SettingCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = modifier.padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}


@Composable
fun AddTagDialog(onClose: () -> Unit, onAdd: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }

    ActionDialog(title = stringResource(R.string.add_tag), actions = {
        TextButton(onClick = onClose) {
            Text(stringResource(R.string.close))
        }
        TextButton(onClick = { onClose(); onAdd(text) }) {
            Text(stringResource(R.string.add))
        }
    }, onDismissRequest = onClose) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.tag)) },
            modifier = Modifier
                .fillMaxWidth()
        )
        // TODO autocomplete
    }
}

@Composable
@Preview
fun AddTagDialogPreview() {
    AddTagDialog({}, {})
}

class SearchScreenState(
    initialSearchOptions: SearchOptions,
    openDialog: Boolean = false
) {
    val tags = mutableStateListOf<String>().also { it.addAll(initialSearchOptions.tags) }
    var order by mutableStateOf(initialSearchOptions.order)
    var orderAscending by mutableStateOf(initialSearchOptions.orderAscending)
    var rating = mutableStateListOf<Rating>().also { it.addAll(initialSearchOptions.rating) }

    var openDialog by mutableStateOf(openDialog)

    fun makeSearchOptions(): SearchOptions =
        SearchOptions(ArrayList(tags), order, orderAscending, rating)

    companion object {
        val Saver: Saver<SearchScreenState, Bundle> = Saver(
            save = { state ->
                val bundle = Bundle()
                bundle.putString(
                    "tags",
                    state.tags.joinToString(",")
                ) // Couldn't use putStringArrayList because restore constructor used with Navigation
                bundle.putString("order", state.order.name)
                bundle.putBoolean("ascending", state.orderAscending)
                bundle.putString("rating", state.rating.joinToString(",") { it.name })
                bundle.putBoolean("openDialog", state.openDialog)
                return@Saver bundle
            },
            restore = { bundle ->
                val searchOptions = SearchOptions(bundle)
                return@Saver SearchScreenState(searchOptions, bundle.getBoolean("openDialog"))
            }
        )
    }
}

@Composable
fun Search(
    initialSearchOptions: SearchOptions,
    onSearch: (SearchOptions) -> Unit
) {
    val state = rememberSaveable(initialSearchOptions, saver = SearchScreenState.Saver) {
        SearchScreenState(initialSearchOptions)
    }

    if (state.openDialog) {
        AddTagDialog(onClose = { state.openDialog = false }, onAdd = { state.tags.add(it) })
    }

    Base {
        Spacer(modifier = Modifier.height(4.dp))
        SettingCard(title = stringResource(R.string.tags)) {
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                for (tag in state.tags) {
                    key(tag) {
                        OutlinedChip(modifier = Modifier.padding(4.dp)) {
                            Text(tag)
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.remove),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { state.tags.remove(tag) }
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
            TextButton(
                onClick = { state.openDialog = true },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(stringResource(R.string.add_tag))
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        SettingCard(title = stringResource(R.string.order), modifier = Modifier.selectableGroup()) {
            for (v in Order.values()) {
                key(v.apiName) {
                    val selected = v == state.order
                    val onClick: () -> Unit = {
                        state.order = v
                        if (!v.supportsAscending) state.orderAscending = false
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .selectable(
                                selected = selected,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClick
                            )
                            .fillMaxWidth()
                    ) {
                        RadioButton(
                            modifier = Modifier.padding(start = 8.dp),
                            selected = selected,
                            onClick = onClick,
                            colors = RadioButtonDefaults.colors(MaterialTheme.colors.primary)
                        )
                        Text(
                            text = stringResource(v.descriptionId),
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = state.order.supportsAscending
                    ) {
                        state.orderAscending = !state.orderAscending
                    }
                    .fillMaxWidth()
            ) {
                Checkbox(
                    enabled = state.order.supportsAscending,
                    checked = state.orderAscending,
                    modifier = Modifier.padding(8.dp),
                    onCheckedChange = { state.orderAscending = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary)
                )
                Text(
                    text = stringResource(R.string.order_ascending),
                    modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                    color = if (state.order.supportsAscending) Color.Unspecified else Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        SettingCard(title = stringResource(R.string.rating)) {
            key(null) {
                val selected = state.rating.size == 3
                val onClick: () -> Unit = {
                    state.rating.clear()
                    state.rating.addAll(Rating.values().toList())
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.selectable(
                        selected = selected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                ) {
                    RadioButton(
                        modifier = Modifier.padding(start = 8.dp),
                        selected = selected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(MaterialTheme.colors.primary)
                    )
                    Text(
                        text = stringResource(R.string.any),
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                    )
                }
            }
            for (v in Rating.values()) {
                key(v.apiName) {
                    val value = v in state.rating && state.rating.size != 3
                    val onValueChange: (Boolean) -> Unit = {
                        when {
                            state.rating.size == 3 -> state.rating.apply {
                                clear()
                                add(v)
                            }
                            v in state.rating -> state.rating.remove(v)
                            else -> state.rating.add(v)
                        }
                        if (state.rating.isEmpty()) state.rating.addAll(Rating.values())
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.toggleable(
                            value = value,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onValueChange = onValueChange
                        )
                    ) {
                        Checkbox(
                            modifier = Modifier.padding(start = 8.dp),
                            checked = value,
                            onCheckedChange = onValueChange,
                            colors = CheckboxDefaults.colors(MaterialTheme.colors.primary)
                        )
                        Text(
                            text = stringResource(v.descriptionId),
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = {
                onSearch(state.makeSearchOptions())
            },
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            Text(stringResource(R.string.search))
            Icon(Icons.Rounded.NavigateNext, contentDescription = null)
        }
    }
}