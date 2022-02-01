package ru.herobrine1st.e621.ui

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
    var text by remember { mutableStateOf("") }
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

@Composable
fun Search(onSearch: (SearchOptions) -> Unit) {
    var openDialog by remember { mutableStateOf(false) }

    // Tags
    val tags = remember { mutableStateListOf<String>() }
    // Order
    var order: Order by remember { mutableStateOf(Order.NEWEST_TO_OLDEST) }
    var orderAscending by remember { mutableStateOf(false) }
    var supportsAscending by remember { mutableStateOf(true) }

    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    var pagingAllowed by remember { mutableStateOf(true) } // TODO добавить "со страницы"
    // Rating
    var rating: Rating by remember { mutableStateOf(Rating.ANY) }

    if (openDialog) {
        AddTagDialog(onClose = { openDialog = false }, onAdd = { tags.add(it) })
    }

    Base {
        Spacer(modifier = Modifier.height(4.dp))
        SettingCard(title = stringResource(R.string.tags)) {
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                for (tag in tags) {
                    key(tag) {
                        OutlinedChip(modifier = Modifier.padding(4.dp)) {
                            Text(tag)
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.remove),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { tags.remove(tag) }
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
            TextButton(
                onClick = { openDialog = true },
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.selectable(
                            selected = v == order,
                            onClick = {
                                order = v
                                supportsAscending = v.supportsAscending
                                pagingAllowed = v.supportsPaging
                                if (!v.supportsAscending) orderAscending = false
                            }
                        )
                    ) {
                        RadioButton(
                            modifier = Modifier.padding(start = 8.dp),
                            selected = v == order,
                            onClick = {
                                order = v
                                supportsAscending = v.supportsAscending
                                pagingAllowed = v.supportsPaging
                                if (!v.supportsAscending) orderAscending = false
                            })
                        Text(
                            text = stringResource(v.descriptionId),
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = supportsAscending) {
                    if (supportsAscending) orderAscending = !orderAscending
                }
            ) {
                Checkbox(
                    enabled = supportsAscending,
                    checked = orderAscending,
                    modifier = Modifier.padding(8.dp),
                    onCheckedChange = { if (supportsAscending) orderAscending = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary)
                )
                Text(
                    text = stringResource(R.string.order_ascending),
                    modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                    color = if (supportsAscending) Color.Unspecified else Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        SettingCard(title = stringResource(R.string.rating)) {
            for (v in Rating.values()) {
                key(v.apiName) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.selectable(
                            selected = v == rating,
                            onClick = {
                                rating = v
                            }
                        )
                    ) {
                        RadioButton(
                            modifier = Modifier.padding(start = 8.dp),
                            selected = v == rating,
                            onClick = {
                                rating = v
                            })
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
                onSearch(SearchOptions(tags, order, orderAscending, rating))
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