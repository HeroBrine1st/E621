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

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.autocomplete.AutocompleteInputField
import ru.herobrine1st.autocomplete.AutocompleteInputFieldDefaults
import ru.herobrine1st.autocomplete.AutocompleteSearchResult
import ru.herobrine1st.autocomplete.rememberAutocompleteState
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.Tokens
import ru.herobrine1st.e621.navigation.component.search.SearchComponent.TagSuggestion
import ru.herobrine1st.e621.ui.dialog.ActionDialog
import ru.herobrine1st.e621.util.runIf
import ru.herobrine1st.e621.util.text

private const val TAG = "ModifyTagDialog"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyTagDialog(
    initialText: String,
    getSuggestionsFlow: (() -> String) -> Flow<AutocompleteSearchResult<TagSuggestion>>,
    onClose: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onApply: (String) -> Unit,
) {
    val autocompleteState = rememberAutocompleteState(
        initialItem = initialText,
        // INVARIANCE: initialText parameter is re-used as initial value in produceState
        initialText = initialText,
        transformToInputText = { it },
    )

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }


    val suggestions by produceState(AutocompleteSearchResult.Ready(emptyList(), initialText)) {
        getSuggestionsFlow { autocompleteState.currentText }.collect {
            value = it
        }
    }

    fun apply() {
        val text = autocompleteState.currentText
        if (' ' in text)
            Log.wtf(TAG, "Found spaces in tag, which should not be possible: ${autocompleteState.currentText}")
        val result = text.lowercase().trimEnd('_')
        when {
            result.isNotBlank() -> onApply(result)
            onDelete != null -> onDelete()
            else -> onClose()
        }
    }

    ActionDialog(
        title = stringResource(R.string.add_tag),
        actions = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close))
            }
            if (onDelete != null) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red,
                    ),
                ) {
                    Text(stringResource(R.string.remove))
                }
            }
            TextButton(onClick = ::apply) {
                Text(stringResource(if (onDelete == null) R.string.add else R.string.apply))
            }
        },
        onDismissRequest = onClose,
    ) {
        AutocompleteInputField(
            autocompleteState,
            suggestions = { suggestions },
            transformToSelectedItem = { it.name.value },
            suggestedItem = { item ->
                val name = item.name.text
                val suggestionText = when (item.antecedentName) {
                    null -> name
                    else -> item.antecedentName.text + " → " + name
                }
                DropdownMenuItem(
                    text = { Text(suggestionText, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        val prefix = listOf(Tokens.ALTERNATIVE, Tokens.EXCLUDED).find { token ->
                            autocompleteState.currentText.startsWith(token)
                        } ?: ""
                        autocompleteState.selectItem(prefix + item.name.value)
                    },
                )
            },
            textField = { modifier ->
                OutlinedTextField(
                    value = autocompleteState.currentTextValue,
                    onValueChange = { value ->
                        autocompleteState.onValueChange(
                            value.copy(
                                text = value.text.lowercase().replace(' ', '_').let { query ->
                                    // sanitize input
                                    val prefix = listOf(Tokens.ALTERNATIVE, Tokens.EXCLUDED).find { token ->
                                        query.startsWith(token)
                                    } ?: ""
                                    prefix + query.removePrefix(prefix)
                                        .let { it1 ->
                                            // remove additional tokens
                                            var res = it1
                                            while (res.startsWith(Tokens.EXCLUDED) || res.startsWith(Tokens.ALTERNATIVE)) {
                                                res = res.removePrefix(Tokens.ALTERNATIVE)
                                                    .removePrefix(Tokens.EXCLUDED)
                                            }
                                            res
                                        }
                                        .trimStart('_')
                                },
                            ),
                        )
                    },
                    label = { Text(stringResource(R.string.tag)) },
                    singleLine = true,
                    modifier = modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardActions = KeyboardActions { apply() },
                    visualTransformation = {
                        TransformedText(
                            text = AnnotatedString(
                                it.text.runIf(BuildConfig.HIDE_UNDERSCORES_FROM_USER) {
                                    replace('_', ' ')
                                },
                            ),
                            offsetMapping = OffsetMapping.Identity,
                        )
                    },
                    trailingIcon = {
                        AutocompleteInputFieldDefaults.DefaultTrailingIcon(
                            autocompleteState,
                            enabled = true,
                            withArrowIcon = true,
                            suggestions = { suggestions },
                        )
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                )
            },
        )
    }
}

