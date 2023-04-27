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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R

@Composable
fun DisclaimerDialog(
    text: @Composable () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    icon: @Composable () -> Unit = {
        Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.warning))
    },
    title: @Composable () -> Unit = {
        Text(stringResource(R.string.disclaimer))
    },
    confirmButton: @Composable () -> Unit = {
        Button(onClick = onApply) {
            Text(stringResource(R.string.i_understand))
        }
    },
    dismissButton: @Composable () -> Unit = {
        FilledTonalButton(onClick = onDismiss) {
            Text(stringResource(R.string.dialog_dismiss))
        }
    }
) {
    AlertDialog(
        icon = icon,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        onDismissRequest = onDismiss
    )
}