/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CodeOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.AutocompleteSuggestionsAPI
import ru.herobrine1st.e621.api.model.Order
import ru.herobrine1st.e621.api.model.PostId
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.api.model.SimpleFileType
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.api.model.TagAutocompleteSuggestion
import ru.herobrine1st.e621.api.search.PostsSearchOptions
import ru.herobrine1st.e621.module.CachedDataStore
import ru.herobrine1st.e621.module.DataStoreModule
import ru.herobrine1st.e621.navigation.component.search.SearchComponent
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.MenuAction
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState
import ru.herobrine1st.e621.ui.component.scaffold.rememberScreenPreviewSharedState
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.e621.util.PreviewUtils
import ru.herobrine1st.e621.util.getPreviewComponentContext
import ru.herobrine1st.e621.util.getPreviewStackNavigator
import ru.herobrine1st.e621.util.text

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, CachedDataStore::class)
@Composable
fun Search(
    screenSharedState: ScreenSharedState,
    component: SearchComponent,
) {
    val tagModificationState = rememberSaveable(saver = TagModificationState.Saver) {
        mutableStateOf(
            TagModificationState.None,
        )
    }

    // This dialog will cause too many recompositions so better isolate it in its own restartable function
    TagModificationDialog(tagModificationState, component)

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
                        onOpenBlacklistDialog = screenSharedState.openBlacklistDialog,
                    ) { close ->
                        // remember so that it doesn't change while collapsing
                        when (val queryPresentation = remember { component.presentedQuery }) {
                            is SearchComponent.QueryPresentation.Raw -> if (queryPresentation.canTransformToTagList) {
                                MenuAction(
                                    Icons.Default.CodeOff,
                                    stringResource(R.string.search_screen_disable_raw_query),
                                ) {
                                    queryPresentation.toTagList()?.let { component.presentedQuery = it }
                                    close()
                                }
                            }

                            is SearchComponent.QueryPresentation.TagList -> {
                                MenuAction(
                                    Icons.Default.Code,
                                    stringResource(R.string.search_screen_enable_raw_query),
                                ) {
                                    component.presentedQuery = queryPresentation.toRaw()
                                    close()
                                }
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
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
                onClick = component::proceed,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = paddingValues,
        ) {
            item {}
            item("tags") {
                SettingCard(title = stringResource(R.string.tags)) {
                    when (val presentedQuery = component.presentedQuery) {
                        is SearchComponent.QueryPresentation.Raw -> {
//                            val autocompleteState: AutocompleteState<String>
//                            autocompleteState = rememberAutocompleteState(
//                                initialItem = presentedQuery.query,
//                                initialText = presentedQuery.query,
//                                transformToInputText = { suggestion ->
//                                    // TODO pass full state here and make it return TextFieldValue
//                                    // (looks like it requires full API rewrite since transformToInputText is called immediately to get TextFieldValue
//                                    //  so currently not possible)
//                                    val textFieldValue = autocompleteState.currentTextValue
//                                    val cursorPosition = textFieldValue.selection.start
//                                    val currentQuery = textFieldValue.text.takeLast(cursorPosition).substringAfter(" ")
//                                    textFieldValue.copy(
//                                        text = textFieldValue.text.substring(0, cursorPosition - currentQuery.length) +
//                                                suggestion +
//                                                textFieldValue.text.substring(cursorPosition),
//                                        selection = TextRange(textFieldValue.selection.start + suggestion.length - currentQuery.length),
//                                    ).text
//                                },
//                            )
//
//                            val suggestions by produceState(
//                                AutocompleteSearchResult.Ready(
//                                    emptyList(),
//                                    presentedQuery.query.substringAfter(" "),
//                                ),
//                            ) {
//                                component.tagSuggestionFlow {
//                                    val textFieldValue = autocompleteState.currentTextValue
//                                    textFieldValue.selection.let {
//                                        if (!it.collapsed) return@tagSuggestionFlow ""
//                                    }
//                                    // take everything before cursor
//                                    textFieldValue.text.takeLast(textFieldValue.selection.start).substringAfter(" ")
//                                }.collect {
//                                    value = it
//                                }
//                            }
//
//                            AutocompleteInputField(
//                                autocompleteState,
//                                suggestions = { suggestions },
//                                transformToSelectedItem = { it.name.value },
//                                suggestedItem = { item ->
//                                    val name = item.name.text
//                                    val suggestionText = when (item.antecedentName) {
//                                        null -> name
//                                        else -> item.antecedentName.text + " → " + name
//                                    }
//                                    DropdownMenuItem(
//                                        text = { Text(suggestionText, style = MaterialTheme.typography.bodyLarge) },
//                                        onClick = {
//                                            val prefix = listOf(Tokens.ALTERNATIVE, Tokens.EXCLUDED).find { token ->
//                                                autocompleteState.currentText.startsWith(token)
//                                            } ?: ""
//                                            autocompleteState.selectItem(prefix + item.name.value)
//                                        },
//                                    )
//                                },
//                                textField = { modifier ->
                            OutlinedTextField(
//                                        value = autocompleteState.currentTextValue,
//                                        onValueChange = autocompleteState::onValueChange,
                                value = presentedQuery.query,
                                onValueChange = {
                                    component.presentedQuery = presentedQuery.copy(query = it)
                                },
                                label = { Text(stringResource(R.string.search_screen_raw_query_input_field)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
//                                        trailingIcon = {
//                                            AutocompleteInputFieldDefaults.DefaultTrailingIcon(
//                                                autocompleteState,
//                                                enabled = true,
//                                                withArrowIcon = true,
//                                                suggestions = { suggestions },
//                                            )
//                                        },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done,
                                ),
                            )
//                                },
//                            )
                        }

                        is SearchComponent.QueryPresentation.TagList -> {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                presentedQuery.tags.forEachIndexed { index, tag ->
                                    key(tag) {
                                        InputChip(
                                            selected = false,
                                            onClick = {
                                                tagModificationState.value = TagModificationState.Editing(index)
                                            },
                                            label = {
                                                Text(Tag(tag).text)
                                            },
                                        )
                                    }
                                }
                            }
                            TextButton(
                                onClick = { tagModificationState.value = TagModificationState.AddingNew },
                                modifier = Modifier.align(Alignment.Start),
                            ) {
                                Text(stringResource(R.string.add_tag))
                                Icon(Icons.Rounded.Add, contentDescription = null)
                            }
                        }
                    }
                }
            }
            item("order") {
                SettingCard(
                    title = stringResource(R.string.order),
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            singleLine = true,
                            value = stringResource(component.order.descriptionId),
                            onValueChange = {},
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    null,
                                    Modifier
                                        .rotate(
                                            animateFloatAsState(
                                                if (expanded) 180f else 360f,
                                                label = "dropdown arrow rotation animation",
                                            ).value,
                                        )
                                        .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
                                )
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.exposedDropdownSize(matchAnchorWidth = true),
                        ) {
                            Order.entries.forEach {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(it.descriptionId),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    },
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
                        enabled = component.order.ascendingApiName != null,
                        text = stringResource(R.string.order_ascending),
                    ) {
                        component.orderAscending = !component.orderAscending
                    }
                }
            }
            item("rating") {
                SettingCard(
                    title = stringResource(R.string.rating),
                    modifier = Modifier.selectableGroup(),
                ) {
                    AnimatedVisibility(visible = component.shouldBlockRatingChange) {
                        Text(stringResource(R.string.search_safe_mode))
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        CompositionLocalProvider(LocalRippleConfiguration provides null) {
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
                                    enabled = !component.shouldBlockRatingChange,
                                    leadingIcon = {
                                        AnimatedVisibility(
                                            visible = selected,
                                            enter = fadeIn() + expandIn(expandFrom = Alignment.CenterStart),
                                            exit = shrinkOut(shrinkTowards = Alignment.CenterStart) + fadeOut(),
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item("file type") {
                SettingCard(
                    title = stringResource(R.string.post_type),
                    modifier = Modifier.selectableGroup(),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        CompositionLocalProvider(LocalRippleConfiguration provides null) {
                            for (type in SimpleFileType.entries) {
                                val selected = type in component.postTypes
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected)
                                            component.postTypes.remove(type)
                                        else
                                            component.postTypes.add(type)
                                        // Do not force any behavior: users are free to select all
                                        // or select none as it is the same
                                    },
                                    label = {
                                        Text(
                                            when (type) {
                                                SimpleFileType.IMAGE -> stringResource(R.string.post_type_image)
                                                SimpleFileType.ANIMATION -> stringResource(R.string.post_type_animation)
                                                SimpleFileType.VIDEO -> stringResource(R.string.post_type_video)
                                            },
                                        )
                                    },
                                    leadingIcon = {
                                        AnimatedVisibility(
                                            visible = selected,
                                            enter = fadeIn() + expandIn(expandFrom = Alignment.CenterStart),
                                            exit = shrinkOut(shrinkTowards = Alignment.CenterStart) + fadeOut(),
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item("additional chips") {
                SettingCard(title = stringResource(R.string.search_screen_additional_filters)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        var favouritesOfDialog by remember { mutableStateOf(false) }
                        var postIdDialog by remember { mutableStateOf(false) }
                        var poolIdDialog by remember { mutableStateOf(false) }

                        val favouritesOfTransition =
                            updateTransition(component.favouritesOf.isNotEmpty(), "FavouritesOf chip transition")
                        val postIdTransition =
                            updateTransition(component.parentPostId != PostId.INVALID, "PostId chip transition")
                        val poolIdTransition =
                            updateTransition(component.poolId != -1, "PoolId chip transition")

                        // targetState is synchronously updated like rememberUpdatedState

                        FilterChip(
                            selected = favouritesOfTransition.targetState,
                            onClick = {
                                if (favouritesOfTransition.targetState) component.favouritesOf = ""
                                else favouritesOfDialog = true
                            },
                            label = {
                                favouritesOfTransition.AnimatedContent(
                                    transitionSpec = {
                                        fadeIn() togetherWith fadeOut() using SizeTransform()
                                    },
                                ) { set ->
                                    // remember so doesn't change to blank
                                    val favouritesOf = remember { component.favouritesOf }
                                    Text(
                                        when {
                                            set && favouritesOf == component.accountName -> stringResource(
                                                R.string.search_screen_favourites_of_input_chip_yourself,
                                            )

                                            set -> stringResource(
                                                R.string.search_screen_favourites_of_input_chip,
                                                favouritesOf,
                                            )

                                            else -> stringResource(R.string.search_screen_favourites_of_input_chip_disabled)
                                        },
                                    )
                                }
                            },
                            leadingIcon = {
                                favouritesOfTransition.AnimatedVisibility(
                                    visible = { it },
                                    enter = fadeIn() + expandIn(expandFrom = Alignment.CenterStart),
                                    exit = shrinkOut(shrinkTowards = Alignment.CenterStart) + fadeOut(),
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            },
                        )

                        FilterChip(
                            selected = postIdTransition.targetState,
                            onClick = {
                                if (postIdTransition.targetState) component.parentPostId = PostId.INVALID
                                else postIdDialog = true
                            },
                            label = {
                                postIdTransition.AnimatedContent(
                                    transitionSpec = {
                                        fadeIn() togetherWith fadeOut() using SizeTransform()
                                    },
                                ) { set ->
                                    Text(
                                        if (set) stringResource(
                                            R.string.search_screen_child_of_parent_input_chip,
                                            // remember so doesn't change to -1
                                            remember { component.parentPostId.value },
                                        ) else stringResource(R.string.search_screen_child_of_parent_input_chip_disabled),
                                    )
                                }
                            },
                            leadingIcon = {
                                postIdTransition.AnimatedVisibility(
                                    visible = { it },
                                    enter = fadeIn() + expandIn(expandFrom = Alignment.CenterStart),
                                    exit = shrinkOut(shrinkTowards = Alignment.CenterStart) + fadeOut(),
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            },
                        )

                        FilterChip(
                            selected = poolIdTransition.targetState,
                            onClick = {
                                if (!poolIdTransition.targetState) poolIdDialog = true
                                else component.poolId = -1
                            },
                            label = {
                                poolIdTransition.AnimatedContent(
                                    transitionSpec = {
                                        fadeIn() togetherWith fadeOut() using SizeTransform()
                                    },
                                ) { set ->
                                    Text(
                                        if (set) stringResource(
                                            R.string.search_screen_pool_input_chip,
                                            // remember so doesn't change to -1
                                            remember { component.poolId },
                                        ) else stringResource(R.string.search_screen_pool_input_chip_disabled),
                                    )
                                }
                            },
                            leadingIcon = {
                                poolIdTransition.AnimatedVisibility(
                                    { it },
                                    enter = fadeIn() + expandIn(expandFrom = Alignment.CenterStart),
                                    exit = shrinkOut(shrinkTowards = Alignment.CenterStart) + fadeOut(),
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            },
                        )

                        if (favouritesOfDialog) {
                            UsernameInputDialog(
                                onDismissRequest = { favouritesOfDialog = false },
                                usernameMyself = component.accountName,
                                onApply = {
                                    component.favouritesOf = it
                                    favouritesOfDialog = false
                                },
                            )
                        } else if (postIdDialog) {
                            NaturalNumberInputDialog(
                                onDismissRequest = { postIdDialog = false },
                                title = { Text(stringResource(R.string.post_id)) },
                                onApply = {
                                    component.parentPostId = PostId(it)
                                    postIdDialog = false
                                },
                            )
                        } else if (poolIdDialog) {
                            NaturalNumberInputDialog(
                                onDismissRequest = { poolIdDialog = false },
                                title = { Text(stringResource(R.string.search_pool_id)) },
                                onApply = {
                                    component.poolId = it
                                    poolIdDialog = false
                                },
                            )
                        }
                    }
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
    Search(
        screenSharedState = rememberScreenPreviewSharedState(),
        component = SearchComponent(
            getPreviewComponentContext(),
            getPreviewStackNavigator(),
            PostsSearchOptions(
                query = "test test2 test3",
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
            dataStoreModule = DataStoreModule(LocalContext.current.applicationContext),
        ),
    )
}

@Composable
private fun TagModificationDialog(
    tagModificationState: MutableState<TagModificationState>,
    component: SearchComponent,
) {
    var tagModificationState by tagModificationState
    val presentedQuery = component.presentedQuery
    if (presentedQuery is SearchComponent.QueryPresentation.TagList) when (val state = tagModificationState) {
        TagModificationState.None -> {}
        is TagModificationState.Editing, TagModificationState.AddingNew -> {
            ModifyTagDialog(
                (state as? TagModificationState.Editing)?.index?.let { presentedQuery.tags[it] } ?: "",
                getSuggestionsFlow = component::tagSuggestionFlow,
                onClose = {
                    tagModificationState = TagModificationState.None
                },
                onDelete = if (state is TagModificationState.Editing) fun() {
                    component.presentedQuery =
                        presentedQuery.copy(tags = presentedQuery.tags.toMutableList().apply { removeAt(state.index) })
                    tagModificationState = TagModificationState.None
                } else null,
                onApply = {
                    component.presentedQuery = presentedQuery.copy(
                        tags = presentedQuery.tags.toMutableList().apply {
                            when (state) {
                                is TagModificationState.Editing -> set(state.index, it)
                                is TagModificationState.AddingNew -> add(it)
                            }
                        },
                    )
                    tagModificationState = TagModificationState.None
                },
            )
        }
    }
}

@Composable
private fun NaturalNumberInputDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    onApply: (Int) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = {
            OutlinedTextField(
                value,
                onValueChange = { input ->
                    if (input.toIntOrNull()?.takeIf { it > 0 } != null || input.isEmpty()) value = input
                },
                modifier = Modifier.focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(value.toIntOrNull() ?: return@TextButton)
                },
                enabled = value.toIntOrNull() != null,
            ) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_dismiss))
            }
        },
    )
}

@Composable
private fun UsernameInputDialog(
    onDismissRequest: () -> Unit,
    usernameMyself: String?,
    onApply: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(R.string.favourite_of))
        },
        text = {
            OutlinedTextField(
                value,
                onValueChange = { input ->
                    value = input
                },
                label = { Text(stringResource(R.string.user)) },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            value = if (usernameMyself != null && value.isEmpty()) usernameMyself else ""
                        },
                    ) {
                        if (usernameMyself != null && value.isEmpty()) Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.search_fill_myself),
                        ) else Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear),
                        )
                    }
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (value.isNotBlank()) onApply(value.trim())
                },
                enabled = value.isNotBlank(),
            ) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_dismiss))
            }
        },
    )
}