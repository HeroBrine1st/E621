package ru.herobrine1st.e621.ui

import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.flowlayout.FlowRow
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.Order
import ru.herobrine1st.e621.api.Rating
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.OutlinedChip


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
                )
            ) {
                Text(
                    text = stringResource(R.string.add_tag),
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.tag)) },
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .padding(all = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            onClose()
                            onAdd(text)
                        }
                    ) {
                        Text(stringResource(R.string.add))
                    }
                }
            }
        }
    }
}

data class SearchOptions(
    val tags: List<String>,
    val order: Order,
    val orderAscending: Boolean,
    val rating: Rating
)

val defaultSearchOptions = SearchOptions(emptyList(), Order.NEWEST_TO_OLDEST, false, Rating.ANY)

class SearchScreenState(
    initialSearchOptions: SearchOptions = defaultSearchOptions,
    openDialog: Boolean = false
) {
    val tags = mutableStateListOf<String>().also { it.addAll(initialSearchOptions.tags) }
    var order by mutableStateOf(initialSearchOptions.order)
    var orderAscending by mutableStateOf(initialSearchOptions.orderAscending)
    var rating by mutableStateOf(initialSearchOptions.rating)

    var openDialog by mutableStateOf(openDialog)

    fun makeSearchOptions(): SearchOptions =
        SearchOptions(ArrayList(tags), order, orderAscending, rating)

    companion object {
        val Saver: Saver<SearchScreenState, Bundle> = Saver(
            save = {
                val bundle = Bundle()
                bundle.putStringArrayList("tags", ArrayList(it.tags))
                bundle.putString("order", it.order.name)
                bundle.putBoolean("orderAscending", it.orderAscending)
                bundle.putString("rating", it.rating.name)
                bundle.putBoolean("openDialog", it.openDialog)
                return@Saver bundle
            },
            restore = {
                val searchOptions = SearchOptions(
                    it.getStringArrayList("tags")!!,
                    Order.valueOf(it.getString("order")!!),
                    it.getBoolean("orderAscending"),
                    Rating.valueOf(it.getString("rating")!!)
                )
                return@Saver SearchScreenState(searchOptions, it.getBoolean("openDialog"))
            }
        )
    }
}

@Composable
fun Search(
    initialSearchOptions: SearchOptions = defaultSearchOptions,
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
                        modifier = Modifier.selectable(
                            selected = selected,
                            onClick = onClick
                        )
                    ) {
                        RadioButton(
                            modifier = Modifier.padding(start = 8.dp),
                            selected = selected,
                            onClick = onClick
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
                modifier = Modifier.clickable(enabled = state.order.supportsAscending) {
                    state.orderAscending = !state.orderAscending
                }
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
            for (v in Rating.values()) {
                key(v.apiName) {
                    val selected = v == state.rating
                    val onClick: () -> Unit = {
                        state.rating = v
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.selectable(
                            selected = selected,
                            onClick = onClick
                        )
                    ) {
                        RadioButton(
                            modifier = Modifier.padding(start = 8.dp),
                            selected = selected,
                            onClick = onClick
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