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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.search.SearchComponent
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.ui.dialog.ActionDialog
import ru.herobrine1st.e621.util.runIf
import ru.herobrine1st.e621.util.text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyTagDialog(
    initialText: String,
    getSuggestionsFlow: (() -> String) -> Flow<List<SearchComponent.TagSuggestion>>,
    onClose: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onApply: (String) -> Unit
) {
    val preferences = LocalPreferences.current
    var autocompleteExpanded by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(TextFieldValue(AnnotatedString(initialText))) }

    val suggestions by produceState(emptyList<SearchComponent.TagSuggestion>()) {
        getSuggestionsFlow { textValue.text }.collect {
            value = it
            autocompleteExpanded = it.isNotEmpty()
        }
    }

    ActionDialog(title = stringResource(R.string.add_tag), actions = {
        TextButton(onClick = onClose) {
            Text(stringResource(R.string.close))
        }
        if (onDelete != null) {
            TextButton(
                onClick = onDelete, colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text(stringResource(R.string.remove))
            }
        }
        TextButton(onClick = { onApply(textValue.text) }) {
            Text(stringResource(if (onDelete == null) R.string.add else R.string.apply))
        }
    }, onDismissRequest = onClose) {
        ExposedDropdownMenuBox(
            expanded = autocompleteExpanded,
            onExpandedChange = { autocompleteExpanded = !autocompleteExpanded }
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    if (!autocompleteExpanded)
                        autocompleteExpanded =
                            suggestions.isNotEmpty() // Popup closes on keyboard tap - open it here
                },
                label = { Text(stringResource(R.string.tag)) },
                singleLine = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                keyboardActions = KeyboardActions { onApply(textValue.text) },
                visualTransformation = {
                    TransformedText(
                        text = AnnotatedString(
                            it.text.runIf(BuildConfig.HIDE_UNDERSCORES_FROM_USER) {
                                replace('_', ' ')
                            }
                        ), offsetMapping = OffsetMapping.Identity
                    )
                },
                trailingIcon = {
                    if (preferences.autocompleteEnabled) Icon(
                        Icons.Filled.ArrowDropDown,
                        null,
                        Modifier.rotate(
                            animateFloatAsState(if (autocompleteExpanded) 180f else 360f).value
                        )
                    )
                },
            )
            ExposedDropdownMenu(
                expanded = autocompleteExpanded && suggestions.isNotEmpty(),
                onDismissRequest = {
                    autocompleteExpanded = false
                },
                modifier = Modifier
                    .heightIn(max = (48 * 3).dp) // TODO it still can overlap keyboard
                    .exposedDropdownSize(matchTextFieldWidth = true)
            ) {
                suggestions.forEach {
                    val name = it.name.text
                    val suggestionText = when (it.antecedentName) {
                        null -> name
                        else -> it.antecedentName.text + " → " + name
                    }
                    DropdownMenuItem(
                        text = { Text(suggestionText) },
                        onClick = {
                            textValue = TextFieldValue(AnnotatedString(name))
                            autocompleteExpanded = false
                        })
                }
            }
        }
    }
}

