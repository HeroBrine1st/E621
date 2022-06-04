package ru.herobrine1st.e621.ui.screen.search

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.Order
import ru.herobrine1st.e621.api.Rating
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.OutlinedChip
import ru.herobrine1st.e621.util.PostsSearchOptions

@Composable
fun Search(
    initialPostsSearchOptions: PostsSearchOptions,
    onSearch: (PostsSearchOptions) -> Unit
) {
    val state = rememberSaveable(initialPostsSearchOptions, saver = SearchScreenState.Saver) {
        SearchScreenState(initialPostsSearchOptions)
    }

    if (state.openAddTagDialog) {
        AddTagDialog(onClose = { state.openAddTagDialog = false }, onAdd = { state.tags.add(it) })
    }

    Base(modifier = Modifier.verticalScroll(rememberScrollState(), true)) {
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
                onClick = { state.openAddTagDialog = true },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(stringResource(R.string.add_tag))
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        SettingCard(
            title = stringResource(R.string.order), modifier = Modifier
                .selectableGroup()
        ) {
            var expanded by remember { mutableStateOf(false) }
            val collapsedCount = integerResource(R.integer.order_selection_collapsed_count)
            Order.values()
                .take(collapsedCount)
                .forEach {
                    OrderItem(it, it == state.order) {
                        state.order = it
                        if (!it.supportsAscending) state.orderAscending = false
                    }
                }
            // Split list to 2 lists by selected item and display selected item even if collapsed
            val first: List<Order>
            val second: List<Order>
            val displaySelectedSpecially: Boolean
            Order.values().drop(collapsedCount).let { remaining ->
                val index = remaining.indexOf(state.order)
                if ((index != -1).also { displaySelectedSpecially = it }) {
                    first = remaining.subList(0, index)
                    second = remaining.subList(index + 1, remaining.size)
                } else {
                    first = remaining
                    second = emptyList()
                }
            }
            val onSelect: (Order) -> Unit = {
                state.order = it
                if (!it.supportsAscending) state.orderAscending = false
            }
            OrderSelectionList(first, state.order, expanded, onSelect)
            // Animate exit only if collapsed
            if (expanded) {
                if (displaySelectedSpecially) OrderItem(state.order, true) {}
            } else AnimatedVisibility(
                visible = displaySelectedSpecially,
                enter = fadeIn(initialAlpha = 1f), // Disable
                exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                var item by remember { mutableStateOf(state.order) }
                if(displaySelectedSpecially) item = state.order
                OrderItem(item, true) {}
            }
            if (second.isNotEmpty()) OrderSelectionList(second, state.order, expanded, onSelect)
            ItemSelectionCheckbox(
                checked = state.orderAscending,
                enabled = state.order.supportsAscending,
                text = stringResource(R.string.order_ascending)
            ) {
                state.orderAscending = !state.orderAscending
            }
            TextButton(onClick = { expanded = !expanded }) {
                val rotation: Float by animateFloatAsState(if (expanded) 180f else 360f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) { // To align left
                    Icon(
                        Icons.Default.ExpandMore, null, modifier = Modifier
                            .padding(start = 4.dp, end = 12.dp)
                            .rotate(rotation)
                    )
                    Text(
                        stringResource(if (!expanded) R.string.expand else R.string.collapse)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        SettingCard(title = stringResource(R.string.rating)) {
            key(null) {
                ItemSelectionRadioButton(
                    selected = state.rating.size == 0,
                    text = stringResource(R.string.any)
                ) {
                    state.rating.clear()
                }
            }
            for (v in Rating.values()) {
                ItemSelectionCheckbox(
                    checked = v in state.rating,
                    text = stringResource(v.descriptionId)
                ) {
                    when (v) {
                        in state.rating -> state.rating.remove(v)
                        else -> state.rating.add(v)
                    }
                    if (state.rating.size == Rating.values().size) state.rating.clear()
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        SettingCard(title = stringResource(R.string.favourites_of)) {
            // TODO autocomplete - накостылить через передачу fav:${state.favouritesOf} в autocomplete.json
            OutlinedTextField(
                value = state.favouritesOf,
                onValueChange = { state.favouritesOf = it },
                label = { Text(stringResource(R.string.user)) },
                modifier = Modifier
                    .fillMaxWidth()
            )
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

@Composable
fun OrderSelectionList(
    items: List<Order>,
    selectedItem: Order,
    expanded: Boolean,
    onSelect: (Order) -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
    ) {
        Column {
            items
                .forEach {
                    OrderItem(it, it == selectedItem) {
                        onSelect(it)
                    }
                }
        }
    }
}