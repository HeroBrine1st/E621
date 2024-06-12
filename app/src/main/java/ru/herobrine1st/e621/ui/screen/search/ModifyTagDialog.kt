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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.Tokens
import ru.herobrine1st.e621.navigation.component.search.SearchComponent.Autocomplete
import ru.herobrine1st.e621.ui.dialog.ActionDialog
import ru.herobrine1st.e621.util.runIf
import ru.herobrine1st.e621.util.text

private const val TAG = "ModifyTagDialog"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyTagDialog(
    initialText: String,
    getSuggestionsFlow: (() -> String) -> Flow<Autocomplete>,
    onClose: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onApply: (String) -> Unit,
) {
    var autocompleteExpanded by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(TextFieldValue(AnnotatedString(initialText))) }
    val query by remember {
        derivedStateOf {
            textValue.text
                .removePrefix(Tokens.ALTERNATIVE)
                .removePrefix(Tokens.EXCLUDED)
        }
    }
    // suggestions open again after clicking one
    var selectedFromSuggested by remember { mutableStateOf(false) }

    val autocomplete = produceState<Autocomplete>(Autocomplete.Ready(emptyList(), initialText)) {
        getSuggestionsFlow { query }.collect {
            value = it
            if (!selectedFromSuggested && it is Autocomplete.Ready)
                autocompleteExpanded = it.result.isNotEmpty()
        }
    }.value

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
        TextButton(onClick = {
            if (' ' in textValue.text)
                Log.wtf(TAG, "Found spaces in tag, which should not be possible: ${textValue.text}")
            onApply(textValue.text)
        }) {
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
                    // composition may change after clicking on suggestion
                    // so that suggestions will open again
                    val actuallyChanged =
                        it.text != textValue.text || it.selection != textValue.selection
                    // Single tag in single object
                    textValue = it.copy(text = it.text.replace(' ', '_').let { query ->
                        // sanitize input
                        val prefix =
                            if (query.startsWith(Tokens.ALTERNATIVE)) Tokens.ALTERNATIVE
                            else if (query.startsWith(Tokens.EXCLUDED)) Tokens.EXCLUDED
                            else ""
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
                    })
                    if (!actuallyChanged) return@OutlinedTextField
                    selectedFromSuggested = false
                    if (!autocompleteExpanded && autocomplete is Autocomplete.Ready) {
                        autocompleteExpanded =
                            autocomplete.result.isNotEmpty() // Popup closes on keyboard tap - open it here
                    }
                },
                label = { Text(stringResource(R.string.tag)) },
                singleLine = true,
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
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
                    when (autocomplete) {
                        is Autocomplete.Ready -> if (autocomplete.query != query) {
                            // 24.dp is size of icon
                            // strokeWidth is decreased in proportion (24/40 * 4)
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.4.dp)
                        }

                        is Autocomplete.Error -> Icon(
                            Icons.Default.Error,
                            contentDescription = stringResource(R.string.unknown_error)
                        )
                    }
                }
            )
            ExposedDropdownMenu(
                expanded = autocompleteExpanded && autocomplete is Autocomplete.Ready && autocomplete.result.isNotEmpty(),
                onDismissRequest = {
                    autocompleteExpanded = false
                },
                modifier = Modifier
                    .heightIn(max = (48 * 3).dp) // FIXME it can't overlap keyboard, but instead it does not scroll if it is under keyboard
            ) {
                if (autocomplete !is Autocomplete.Ready) return@ExposedDropdownMenu
                autocomplete.result.forEach {
                    val name = it.name.text
                    val suggestionText = when (it.antecedentName) {
                        null -> name
                        else -> it.antecedentName.text + " → " + name
                    }
                    DropdownMenuItem(
                        text = { Text(suggestionText, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            val prefix = listOf(Tokens.ALTERNATIVE, Tokens.EXCLUDED).find { token ->
                                textValue.text.startsWith(token)
                            } ?: ""

                            autocompleteExpanded = false
                            selectedFromSuggested = true
                            textValue = textValue.copy(
                                annotatedString = AnnotatedString(prefix + it.name.value),
                                selection = TextRange(name.length + prefix.length)
                            )
                        })
                }
            }
        }
    }
}

