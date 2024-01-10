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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.AutocompleteSuggestionsAPI
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.api.model.FileType
import ru.herobrine1st.e621.api.model.Order
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.api.model.TagAutocompleteSuggestion
import ru.herobrine1st.e621.navigation.component.search.SearchComponent
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.preference.proto.PreferencesOuterClass.Preferences
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState
import ru.herobrine1st.e621.ui.component.scaffold.rememberScreenPreviewSharedState
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.e621.util.PreviewUtils
import ru.herobrine1st.e621.util.getPreviewComponentContext
import ru.herobrine1st.e621.util.getPreviewStackNavigator
import ru.herobrine1st.e621.util.text


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Search(
    screenSharedState: ScreenSharedState,
    component: SearchComponent,
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

    var tagModificationState by rememberSaveable(saver = TagModificationState.Saver) {
        mutableStateOf(
            TagModificationState.None
        )
    }

    when (val state = tagModificationState) {
        TagModificationState.None -> {}
        is TagModificationState.Editing, TagModificationState.AddingNew -> {
            ModifyTagDialog(
                (state as? TagModificationState.Editing)?.index?.let { component.tags[it] } ?: "",
                getSuggestionsFlow = component::tagSuggestionFlow,
                onClose = {
                    tagModificationState = TagModificationState.None
                },
                onDelete = if (state is TagModificationState.Editing) fun() {
                    component.tags.removeAt(state.index)
                    tagModificationState = TagModificationState.None
                } else null,
                onApply = if (state is TagModificationState.Editing) fun(it: String) {
                    component.tags[state.index] = it
                    tagModificationState = TagModificationState.None
                } else fun(it: String) {
                    component.tags.add(it)
                    tagModificationState = TagModificationState.None
                }
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.search))
                },
                actions = {
                    ActionBarMenu(
                        onNavigateToSettings = screenSharedState.goToSettings,
                        onOpenBlacklistDialog = screenSharedState.openBlacklistDialog
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = screenSharedState.snackbarHostState)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    Text(stringResource(R.string.search))
                },
                icon = {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                },
                onClick = component::proceed
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = paddingValues
        ) {
            item {}
            item("tags") {
                SettingCard(title = stringResource(R.string.tags)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        component.tags.forEachIndexed { index, tag ->
                            key(tag) {
                                // TODO place it in text field
                                //      https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fm3%2Fimages%2Fkzhfok2g-chip_extra-backspace_3P.mp4?alt=media
                                InputChip(
                                    selected = false,
                                    onClick = {
                                        tagModificationState = TagModificationState.Editing(index)
                                    },
                                    label = {
                                        Text(Tag(tag).text)
                                    }
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { tagModificationState = TagModificationState.AddingNew },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(stringResource(R.string.add_tag))
                        Icon(Icons.Rounded.Add, contentDescription = null)
                    }
                }
            }
            item("order") {
                SettingCard(
                    title = stringResource(R.string.order)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            singleLine = true,
                            value = stringResource(component.order.descriptionId),
                            onValueChange = {},
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    null,
                                    Modifier.rotate(
                                        animateFloatAsState(
                                            if (expanded) 180f else 360f,
                                            label = "dropdown arrow rotation animation"
                                        ).value
                                    )
                                )
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            // FIXME: there's no way to make it fill max width
                            // there was one, but I lost it :-(
                            modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true)
                        ) {
                            Order.entries.forEach {
                                DropdownMenuItem(
                                    text = { Text(stringResource(it.descriptionId)) },
                                    onClick = {
                                        component.order = it
                                        expanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }

                    ItemSelectionCheckbox(
                        checked = component.orderAscending,
                        enabled = component.order.supportsAscending,
                        text = stringResource(R.string.order_ascending)
                    ) {
                        component.orderAscending = !component.orderAscending
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

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (v in Rating.entries) {
                            val selected = v in component.rating
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected)
                                        component.rating.remove(v)
                                    else
                                        component.rating.add(v)
                                    // Do not force any behavior: users are free to select all
                                    // or select none as it is the same
                                },
                                label = { Text(stringResource(v.descriptionId)) },
                                enabled = !preferences.safeModeEnabled,
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Done,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                } else {
                                    null
                                }
                            )
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
                            IconButton(onClick = {
                                // TODO move to component
                                if (component.favouritesOf.isEmpty() && preferences.hasAuth())
                                    component.favouritesOf = preferences.auth.username
                                else component.favouritesOf = ""
                            }) {
                                if (component.favouritesOf.isEmpty() && preferences.hasAuth()) Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = stringResource(R.string.search_fill_myself)
                                ) else Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // TODO make both chips that can be dismissed to clear a field
            item("parent post") {
                SettingCard(title = stringResource(R.string.parent_post)) {
                    OutlinedTextField(
                        value = component.parentPostId.takeIf { it > 0 }?.toString() ?: "",
                        onValueChange = { value ->
                            component.parentPostId = value.toIntOrNull()?.takeIf { it > 0 }
                                ?: return@OutlinedTextField
                        },
                        label = { Text(stringResource(R.string.post_id)) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { component.parentPostId = -1 }) {
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

            item("pool") {
                SettingCard(title = stringResource(R.string.search_pool)) {
                    OutlinedTextField(
                        value = component.poolId.takeIf { it > 0 }?.toString() ?: "",
                        onValueChange = { value ->
                            component.poolId = value.toIntOrNull()?.takeIf { it > 0 }
                                ?: return@OutlinedTextField
                        },
                        label = { Text(stringResource(R.string.search_pool_id)) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { component.poolId = -1 }) {
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
                // minus spacing between items (8 dp)
                // and minus magic 4 dp because 80 dp is too much
                Spacer(Modifier.height(76.dp))
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
            screenSharedState = rememberScreenPreviewSharedState(),
            component = SearchComponent(
                getPreviewComponentContext(),
                getPreviewStackNavigator(),
                PostsSearchOptions(
                    allOf = @Suppress("SpellCheckingInspection") setOf(
                        Tag("asdlkfjaskldjfasdf"),
                        Tag("asddlkfjaslkdjfas"),
                        Tag("test")
                    )
                ),
                api = object : AutocompleteSuggestionsAPI {
                    override suspend fun getAutocompleteSuggestions(query: String, expiry: Int) =
                        Result.success(emptyList<TagAutocompleteSuggestion>())
                },
                exceptionReporter = object : ExceptionReporter {
                    override suspend fun handleRequestException(
                        t: Throwable,
                        message: String,
                        dontShowSnackbar: Boolean,
                        showThrowable: Boolean,
                    ) {
                        // ignore
                    }
                },
                applicationContext = LocalContext.current.applicationContext
            )
        )
    }
}