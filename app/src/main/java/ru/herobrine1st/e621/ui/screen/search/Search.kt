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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.*
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
import ru.herobrine1st.e621.api.model.FileType
import ru.herobrine1st.e621.api.model.Order
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.navigation.component.search.SearchComponent
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.preference.proto.PreferencesOuterClass.Preferences
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffold
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffoldState
import ru.herobrine1st.e621.ui.component.scaffold.rememberPreviewMainScaffoldState
import ru.herobrine1st.e621.util.PreviewUtils
import ru.herobrine1st.e621.util.getPreviewComponentContext
import ru.herobrine1st.e621.util.getPreviewStackNavigator
import ru.herobrine1st.e621.util.normalizeTagForUI


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Search(
    mainScaffoldState: MainScaffoldState,
    component: SearchComponent
) {
    val preferences = LocalPreferences.current

    // TODO move to component
    LaunchedEffect(preferences.safeModeEnabled) {
        if (preferences.safeModeEnabled) {
            component.rating.apply {
                clear()
                add(Rating.SAFE)
            }
        }
    }

    if (component.currentlyModifiedTagIndex == -2) {
        ModifyTagDialog(
            onClose = {
                component.currentlyModifiedTagIndex = -1
            },
            onApply = {
                component.currentlyModifiedTagIndex = -1
                component.tags.add(it)
            }
        )
    } else if (component.currentlyModifiedTagIndex != -1) {
        ModifyTagDialog(
            initialTag = component.tags[component.currentlyModifiedTagIndex],
            onClose = {
                component.currentlyModifiedTagIndex = -1
            },
            onDelete = {
                component.tags.removeAt(component.currentlyModifiedTagIndex)
                component.currentlyModifiedTagIndex = -1
            },
            onApply = {
                component.tags[component.currentlyModifiedTagIndex] = it
                component.currentlyModifiedTagIndex = -1
            }
        )
    }

    MainScaffold(
        state = mainScaffoldState,
        title = { Text(stringResource(R.string.search)) },
        floatingActionButton = {
            FloatingActionButton(onClick = component::proceed) {
                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {}
            item("tags") {
                SettingCard(title = stringResource(R.string.tags)) {
                    FlowRow(mainAxisSpacing = 4.dp) {
                        component.tags.forEachIndexed { index, tag ->
                            key(tag) {
                                Chip(
                                    onClick = {
                                        component.currentlyModifiedTagIndex = index
                                    }
                                ) {
                                    Text(tag.normalizeTagForUI())
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { component.currentlyModifiedTagIndex = -2 },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(stringResource(R.string.add_tag))
                        Icon(Icons.Rounded.Add, contentDescription = null)
                    }
                }
            }
            item("order") {
                SettingCard(
                    title = stringResource(R.string.order),
                    modifier = Modifier.selectableGroup()
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    val collapsedCount = integerResource(R.integer.order_selection_collapsed_count)
                    val onSelect: (Order) -> Unit = {
                        component.order = it
                        if (!it.supportsAscending) component.orderAscending = false
                    }

                    Order.values()
                        .take(collapsedCount)
                        .forEach {
                            OrderItem(it, it == component.order) { onSelect(it) }
                        }
                    //region Split list to 2 lists by selected item and display selected item even if collapsed
                    //           ..list of remaining choices to..
                    val first: List<Order>
                    val second: List<Order>
                    val displaySelectedSpecially: Boolean

                    Order.values().drop(collapsedCount).let { remaining ->
                        val index = remaining.indexOf(component.order)
                        if ((index != -1).also { displaySelectedSpecially = it }) {
                            first = remaining.subList(0, index)
                            second = remaining.subList(index + 1, remaining.size)
                        } else {
                            first = remaining
                            second = emptyList()
                        }
                    }
                    OrderSelectionList(first, component.order, expanded, onSelect)
                    // Animate exit only if collapsed
                    if (expanded) {
                        if (displaySelectedSpecially) OrderItem(component.order, true, onClick = {})
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
                        var item by remember { mutableStateOf(component.order) }
                        if (displaySelectedSpecially) item = component.order
                        OrderItem(item, true, onClick = {})
                    }
                    if (second.isNotEmpty()) OrderSelectionList(
                        second,
                        component.order,
                        expanded,
                        onSelect
                    )
                    ItemSelectionCheckbox(
                        checked = component.orderAscending,
                        enabled = component.order.supportsAscending,
                        text = stringResource(R.string.order_ascending)
                    ) {
                        component.orderAscending = !component.orderAscending
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
            }
            item("rating") {
                SettingCard(
                    title = stringResource(R.string.rating),
                    modifier = Modifier.selectableGroup()
                ) {
                    AnimatedVisibility(visible = preferences.safeModeEnabled) {
                        Text(stringResource(R.string.search_safe_mode))
                    }
                    key(null) {
                        ItemSelectionRadioButton(
                            selected = component.rating.size == 0,
                            text = stringResource(R.string.any),
                            enabled = !preferences.safeModeEnabled
                        ) {
                            component.rating.clear()
                        }
                    }
                    for (v in Rating.values()) {
                        ItemSelectionCheckbox(
                            checked = v in component.rating,
                            text = stringResource(v.descriptionId),
                            enabled = !preferences.safeModeEnabled
                        ) {
                            when (v) {
                                in component.rating -> component.rating.remove(v)
                                else -> component.rating.add(v)
                            }
                            if (component.rating.size == Rating.values().size) component.rating.clear()
                        }
                    }
                }
            }
            item("file type") {
                SettingCard(
                    title = stringResource(R.string.file_type),
                    modifier = Modifier.selectableGroup()
                ) {
                    ItemSelectionRadioButton(
                        selected = component.fileType == null,
                        text = stringResource(R.string.any)
                    ) {
                        component.fileType = null
                        component.fileTypeInvert = false
                    }
                    for (v in FileType.supportedValues()) {
                        ItemSelectionRadioButton(
                            selected = v == component.fileType,
                            text = v.extension
                        ) {
                            component.fileType = v
                        }
                    }
                    ItemSelectionCheckbox(
                        checked = component.fileTypeInvert,
                        text = stringResource(R.string.file_type_invert_selection)
                    ) {
                        component.fileTypeInvert = it
                    }
                }
            }
            item("favourites of") {
                SettingCard(title = stringResource(R.string.favourites_of)) {
                    // TODO autocomplete - накостылить через передачу fav:${state.favouritesOf} в autocomplete.json
                    OutlinedTextField(
                        value = component.favouritesOf,
                        onValueChange = { component.favouritesOf = it },
                        label = { Text(stringResource(R.string.user)) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { component.favouritesOf = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item("size placeholder for fab") {
                // FAB size is 56 dp, plus spacing of fab (16 dp * 2 because we want symmetry)
                // minus spacing between items (4 dp)
                // and minus magic 4 dp because 84 dp is too much
                Spacer(Modifier.height(80.dp))
            }
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
@OptIn(PreviewUtils::class)
fun SearchPreview() {
    CompositionLocalProvider(LocalPreferences provides Preferences.getDefaultInstance()) {
        Search(
            mainScaffoldState = rememberPreviewMainScaffoldState(),
            component = SearchComponent(
                getPreviewComponentContext(),
                getPreviewStackNavigator(),
                PostsSearchOptions.DEFAULT.copy(
                    tags = listOf("asdlkfjaskldjfasdf", "asddl;kfjaslkdjfas;", "test")
                )
            )
        )
    }
}