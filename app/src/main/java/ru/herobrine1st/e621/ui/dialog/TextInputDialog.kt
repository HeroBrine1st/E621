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

package ru.herobrine1st.e621.ui.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R

@Composable
fun TextInputDialog(
    title: String,
    initialText: String = "",
    submitButtonText: String = stringResource(R.string.submit),
    textFieldLabel: String = "",
    singleLine: Boolean = false,
    onClose: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    ActionDialog(
        title = title,
        actions = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close))
            }
            TextButton(onClick = { onSubmit(text); onClose() }) {
                Text(submitButtonText)
            }
        },
        onDismissRequest = onClose
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(textFieldLabel) },
            singleLine = singleLine,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}