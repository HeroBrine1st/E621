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

package ru.herobrine1st.e621.ui.screen.settings.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.preference.proto.PreferencesOuterClass.Preferences
import ru.herobrine1st.e621.preference.proto.ProxyOuterClass.*
import ru.herobrine1st.e621.ui.dialog.ActionDialog

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun ProxyDialog(
    getInitialProxy: () -> Proxy,
    onClose: () -> Unit,
    onApply: (Proxy) -> Unit
) {
    val state = remember { ProxyDialogState(getInitialProxy()) }
    ActionDialog(
        title = stringResource(R.string.proxy_server),
        content = {
            ExposedDropdownMenuBox(
                expanded = state.dropdownExpanded,
                onExpandedChange = {
                    state.dropdownExpanded = !state.dropdownExpanded
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
                                animateFloatAsState(if (state.dropdownExpanded) 180f else 360f).value
                            )
                        )
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        backgroundColor = Color.Transparent
                    )
                )
                ExposedDropdownMenu(
                    expanded = state.dropdownExpanded,
                    onDismissRequest = {
                        state.dropdownExpanded = false
                    }
                ) {
                    ProxyType.values().forEach { type ->
                        DropdownMenuItem(
                            onClick = {
                                state.type = type
                                state.dropdownExpanded = false
                            }, content = {
                                Text(type.toString())
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
                label = { Text(stringResource(R.string.proxy_address)) }
            )
            OutlinedTextField(
                value = if (state.port >= 0) state.port.toString() else "",
                onValueChange = {
                    val port = it.toIntOrNull()
                    if (port != null && port >= 0) state.port = it.toInt()
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
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
                label = { Text(stringResource(R.string.proxy_username)) }
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
                        Crossfade(state.showPassword) { showPassword ->
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        },
        actions = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close))
            }
            TextButton(onClick = { onApply(state.buildProxy()) }, enabled = state.canBuildProxy) {
                Text(stringResource(R.string.apply))
            }
        },
        onDismissRequest = onClose
    )
}

@Preview
@Composable
private fun Preview() {
    MaterialTheme {
        CompositionLocalProvider(LocalPreferences provides Preferences.getDefaultInstance()) {
            ProxyDialog(
                getInitialProxy = { Proxy.getDefaultInstance() },
                onClose = {},
                onApply = {}
            )
        }
    }
}

class ProxyDialogState(
    initialType: ProxyType = ProxyType.SOCKS5,
    initialHostname: String = "",
    initialPort: Int = -1,
    initialUsername: String = "",
    initialPassword: String = ""
) {
    constructor(proxy: Proxy) : this(
        proxy.type,
        proxy.hostname,
        proxy.port,
        proxy.auth.username,
        proxy.auth.password
    )

    var type by mutableStateOf(initialType)
    var hostname by mutableStateOf(initialHostname)
    var port by mutableStateOf(initialPort)
    var username by mutableStateOf(initialUsername)
    var password by mutableStateOf(initialPassword)

    var dropdownExpanded by mutableStateOf(false)
    var showPassword by mutableStateOf(false)

    val canBuildProxy get() = hostname.isNotBlank() && port > 0 && username.isBlank() xor password.isNotBlank()

    fun buildProxy(): Proxy = Proxy.newBuilder().also { proxy ->
        proxy.type = type
        proxy.hostname = hostname
        proxy.port = port
        if (username.isNotBlank() && password.isNotBlank())
            proxy.auth = ProxyAuth.newBuilder().also { auth ->
                auth.username = username
                auth.password = password
            }.build()
    }.build()
}