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

package ru.herobrine1st.e621.ui.screen.search

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.api.model.Order
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.preference.proto.PreferencesOuterClass.Preferences
import ru.herobrine1st.e621.ui.component.Base

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Search(
    initialPostsSearchOptions: PostsSearchOptions,
    onSearch: (PostsSearchOptions) -> Unit
) {
    val state = rememberSaveable(initialPostsSearchOptions, saver = SearchScreenState.Saver) {
        SearchScreenState(initialPostsSearchOptions)
    }

    val preferences = LocalPreferences.current

    LaunchedEffect(preferences.safeModeEnabled) {
        if (preferences.safeModeEnabled) {
            state.rating.apply {
                clear()
                add(Rating.SAFE)
            }
        }
    }

    if (state.currentlyModifiedTagIndex == -2) {
        ModifyTagDialog(
            onClose = {
                state.currentlyModifiedTagIndex = -1
            },
            onApply = {
                state.currentlyModifiedTagIndex = -1
                state.tags.add(it)
            }
        )
    } else if (state.currentlyModifiedTagIndex != -1) {
        ModifyTagDialog(
            initialTag = state.tags[state.currentlyModifiedTagIndex],
            onClose = {
                state.currentlyModifiedTagIndex = -1
            },
            onDelete = {
                state.tags.removeAt(state.currentlyModifiedTagIndex)
                state.currentlyModifiedTagIndex = -1
            },
            onApply = {
                state.tags[state.currentlyModifiedTagIndex] = it
                state.currentlyModifiedTagIndex = -1
            }
        )
    }


    Base(modifier = Modifier.verticalScroll(rememberScrollState(), true)) {
        Spacer(modifier = Modifier.height(4.dp))
        SettingCard(title = stringResource(R.string.tags)) {
            FlowRow(modifier = Modifier.fillMaxWidth(), mainAxisSpacing = 4.dp) {
                state.tags.forEachIndexed { index, tag ->
                    key(tag) {
                        Chip(
                            onClick = {
                                state.currentlyModifiedTagIndex = index
                            }
                        ) {
                            Text(tag)
                        }
                    }
                }
            }
            TextButton(
                onClick = { state.currentlyModifiedTagIndex = -2 },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(stringResource(R.string.add_tag))
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        SettingCard(
            title = stringResource(R.string.order), modifier = Modifier.selectableGroup()
        ) {
            var expanded by remember { mutableStateOf(false) }
            val collapsedCount = integerResource(R.integer.order_selection_collapsed_count)
            val onSelect: (Order) -> Unit = {
                state.order = it
                if (!it.supportsAscending) state.orderAscending = false
            }

            Order.values()
                .take(collapsedCount)
                .forEach {
                    OrderItem(it, it == state.order) { onSelect(it) }
                }
            //region Split list to 2 lists by selected item and display selected item even if collapsed
            //           ..list of remaining choices to..
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
            OrderSelectionList(first, state.order, expanded, onSelect)
            // Animate exit only if collapsed
            if (expanded) {
                if (displaySelectedSpecially) OrderItem(state.order, true, onClick = {})
            } else AnimatedVisibility(
                visible = displaySelectedSpecially,
                enter = fadeIn(initialAlpha = 1f), // Disable
                // Placing an if with "fadeOut(1f)" here (and adding expanded to if below) results
                // in visual glitches, so it is lifted out
                exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) + shrinkVertically(
                    shrinkTowards = Alignment.Top
                ),
            ) {
                // item holds the selected order at the time of selecting
                // so if user wants to select other order, it will collapse as expected by user
                // otherwise (without item) it will collapse with the new selection.
                // Also this prevents some glitches, for example, it could replace one order with
                // selection (or with recently selected order) (I have no reproduce steps)
                var item by remember { mutableStateOf(state.order) }
                if (displaySelectedSpecially) item = state.order
                OrderItem(item, true, onClick = {})
            }
            if (second.isNotEmpty()) OrderSelectionList(second, state.order, expanded, onSelect)
            ItemSelectionCheckbox(
                checked = state.orderAscending,
                enabled = state.order.supportsAscending,
                text = stringResource(R.string.order_ascending)
            ) {
                state.orderAscending = !state.orderAscending
            }
            //endregion
            TextButton(onClick = { expanded = !expanded }) {
                val rotation: Float by animateFloatAsState(if (expanded) 180f else 360f)
                Icon(
                    Icons.Default.ExpandMore, null, modifier = Modifier
                        .padding(start = 4.dp, end = 12.dp)
                        .rotate(rotation)
                )
                Text(
                    stringResource(if (!expanded) R.string.expand else R.string.collapse),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        SettingCard(title = stringResource(R.string.rating)) {
            AnimatedVisibility(visible = preferences.safeModeEnabled) {
                Text(stringResource(R.string.search_safe_mode))
            }
            key(null) {
                ItemSelectionRadioButton(
                    selected = state.rating.size == 0,
                    text = stringResource(R.string.any),
                    enabled = !preferences.safeModeEnabled
                ) {
                    state.rating.clear()
                }
            }
            for (v in Rating.values()) {
                ItemSelectionCheckbox(
                    checked = v in state.rating,
                    text = stringResource(v.descriptionId),
                    enabled = !preferences.safeModeEnabled
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
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { state.favouritesOf = "" }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear)
                        )
                    }
                },
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

@Preview
@Composable
fun SearchPreview() {
    CompositionLocalProvider(LocalPreferences provides Preferences.getDefaultInstance()) {
        Search(initialPostsSearchOptions = PostsSearchOptions.DEFAULT.copy(
            tags = listOf("asdlkfjaskldjfasdf", "asddl;kfjaslkdjfas;", "test", "test")
        ), onSearch = {})
    }
}