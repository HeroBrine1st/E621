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

package ru.herobrine1st.e621.ui.screen.settings.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.preference.Proxy
import ru.herobrine1st.e621.preference.ProxyAuth
import ru.herobrine1st.e621.preference.ProxyType
import ru.herobrine1st.e621.ui.dialog.ActionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyDialog(
    getInitialProxy: () -> Proxy?,
    onClose: () -> Unit,
    onApply: (Proxy) -> Unit
) {
    val state = remember { ProxyDialogState(getInitialProxy()) }
    ActionDialog(
        title = stringResource(R.string.proxy_server),
        actions = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close))
            }
            TextButton(onClick = { onApply(state.buildProxy()) }, enabled = state.canBuildProxy) {
                Text(stringResource(R.string.apply))
            }
        },
        onDismissRequest = onClose
    ) {
        ExposedDropdownMenuBox(
            expanded = state.dropdownExpanded,
            onExpandedChange = {
                state.dropdownExpanded = it
            }
        ) {
            OutlinedTextField(
                readOnly = true,
                singleLine = true,
                value = state.type.toString(),
                onValueChange = {},
                label = { Text(stringResource(R.string.proxy_type)) },
                trailingIcon = {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        null,
                        Modifier.rotate(
                            animateFloatAsState(
                                if (state.dropdownExpanded) 180f else 360f,
                                label = "Dropdown arrow rotation animation"
                            ).value
                        )
                    )
                },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = state.dropdownExpanded,
                onDismissRequest = {
                    state.dropdownExpanded = false
                },
                modifier = Modifier.exposedDropdownSize()
            ) {
                ProxyType.entries.forEach { type ->
                    DropdownMenuItem(
                        onClick = {
                            state.type = type
                            state.dropdownExpanded = false
                        }, text = {
                            Text(type.toString(), style = MaterialTheme.typography.bodyLarge)
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = state.hostname,
            onValueChange = {
                state.hostname = it
            },
            singleLine = true,
            label = { Text(stringResource(R.string.proxy_address)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None
            )
        )
        OutlinedTextField(
            value = if (state.port >= 0) state.port.toString() else "",
            onValueChange = {
                val port = it.toIntOrNull()
                if (port != null && port >= 0) state.port = it.toInt()
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.None
            ),
            singleLine = true,
            label = { Text(stringResource(R.string.proxy_port)) }
        )
        OutlinedTextField(
            value = state.username,
            onValueChange = {
                state.username = it
            },
            singleLine = true,
            label = { Text(stringResource(R.string.proxy_username)) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None
            )
        )
        OutlinedTextField(
            value = state.password,
            visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            onValueChange = {
                state.password = it
            },
            singleLine = true,
            label = { Text(stringResource(R.string.proxy_password)) },
            trailingIcon = {
                IconButton(onClick = { state.showPassword = !state.showPassword }) {
                    Crossfade(
                        state.showPassword,
                        label = "Show password button crossfade"
                    ) { showPassword ->
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                capitalization = KeyboardCapitalization.None
            )
        )
    }
}

@Preview
@Composable
private fun Preview() {
    MaterialTheme {
        ProxyDialog(
            getInitialProxy = { null },
            onClose = {},
            onApply = {}
        )
    }
}

class ProxyDialogState(
    initialType: ProxyType = ProxyType.SOCKS5,
    initialHostname: String = "",
    initialPort: Int = -1,
    initialUsername: String = "",
    initialPassword: String = ""
) {

    companion object {
        operator fun invoke(proxy: Proxy?) = proxy?.let {
            ProxyDialogState(
                proxy.type,
                proxy.hostname,
                proxy.port,
                proxy.auth?.username ?: "",
                proxy.auth?.password ?: ""
            )
        } ?: ProxyDialogState()
    }

    var type by mutableStateOf(initialType)
    var hostname by mutableStateOf(initialHostname)
    var port by mutableIntStateOf(initialPort)
    var username by mutableStateOf(initialUsername)
    var password by mutableStateOf(initialPassword)

    var dropdownExpanded by mutableStateOf(false)
    var showPassword by mutableStateOf(false)

    val canBuildProxy get() = hostname.isNotBlank() && port > 0 && username.isBlank() xor password.isNotBlank()

    fun buildProxy(): Proxy = Proxy(
        type = type,
        hostname = hostname,
        port = port,
        auth = if (username.isNotBlank() && password.isNotBlank()) ProxyAuth(
            username,
            password
        ) else null,
    )
}