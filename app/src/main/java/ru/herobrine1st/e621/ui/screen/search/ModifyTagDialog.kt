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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.dialog.ActionDialog

@Composable
fun ModifyTagDialog(
    initialTag: String? = null,
    onClose: () -> Unit,
    onDelete: () -> Unit = {},
    onApply: (String) -> Unit
) {
    val localInspectionMode = LocalInspectionMode.current
    var text by rememberSaveable {
        mutableStateOf(
            initialTag ?: if (localInspectionMode) "test" else ""
        )
    }

    ActionDialog(title = stringResource(R.string.add_tag), actions = {
        TextButton(onClick = onClose) {
            Text(stringResource(R.string.close))
        }
        if (initialTag != null) {
            TextButton(
                onClick = onDelete, colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text(stringResource(R.string.remove))
            }
        }
        TextButton(onClick = { onApply(text) }) {
            Text(stringResource(if(initialTag == null) R.string.add else R.string.apply))
        }
    }, onDismissRequest = onClose) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.tag)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth(),
            keyboardActions = KeyboardActions { onApply(text); onClose() }
        )
        // TODO autocomplete
    }
}