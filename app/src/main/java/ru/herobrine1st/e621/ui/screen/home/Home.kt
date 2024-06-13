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

package ru.herobrine1st.e621.ui.screen.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.home.IHomeComponent
import ru.herobrine1st.e621.navigation.component.home.IHomeComponent.LoginState
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    screenSharedState: ScreenSharedState,
    component: IHomeComponent
) {
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                },
                actions = {
                    ActionBarMenu(
                        onNavigateToSettings = screenSharedState.goToSettings,
                        onOpenBlacklistDialog = screenSharedState.openBlacklistDialog
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = screenSharedState.snackbarHostState)
        }
    ) {
        Base(Modifier.padding(it)) {
            Button(
                onClick = component::navigateToSearch,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.search))
                Icon(
                    Icons.AutoMirrored.Rounded.NavigateNext,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Crossfade(targetState = component.state, label = "Login layout crossfade") { state ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state) {
                        LoginState.Loading -> CircularProgressIndicator()
                        is LoginState.Authorized -> {

                            FilledTonalButton(
                                onClick = {
                                    showLogoutConfirmation = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Text(text = stringResource(R.string.login_logout))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = component::navigateToFavourites,
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Text(text = stringResource(R.string.favourites))
                            }


                        }

                        LoginState.NoAuth -> AuthorizationMenu { u, p, cb ->
                            component.login(u, p, cb)
                        }

                        LoginState.IOError -> {
                            Text(stringResource(R.string.network_error))
                            Button(onClick = component::retryStoredAuth) {
                                Text(stringResource(R.string.retry))
                            }
                        }

                        LoginState.InternalServerError -> {
                            Text(stringResource(R.string.internal_server_error))
                            Button(
                                onClick = component::retryStoredAuth
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }

                        LoginState.APITemporarilyUnavailable -> {
                            Text(stringResource(R.string.api_temporarily_unavailable))
                            Button(
                                onClick = component::retryStoredAuth
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }

                        LoginState.UnknownAPIError -> {
                            Text(stringResource(R.string.unknown_api_error))
                            Row {
                                Button(onClick = component::retryStoredAuth) {
                                    Text(stringResource(R.string.retry))
                                }
                                Spacer(Modifier.size(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        showLogoutConfirmation = true
                                    },
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text(stringResource(R.string.login_logout))
                                }
                            }
                        }

                        LoginState.UnknownError -> {
                            Text(stringResource(R.string.unknown_error))
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirmation) AlertDialog(
        onDismissRequest = {
            showLogoutConfirmation = false
        },
        title = {
            Text(stringResource(R.string.login_logout_confirmation_dialog_title))
        },
        text = {
            Text(stringResource(R.string.login_logout_confirmation_dialog_text))
        },
        confirmButton = {
            var isLoading by remember { mutableStateOf(false) }
            Button(onClick = {
                isLoading = true
                component.logout {
                    isLoading = false
                    showLogoutConfirmation = false
                }
            }, enabled = !isLoading) {
                Text(stringResource(R.string.login_logout))
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = {
                showLogoutConfirmation = false
            }) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun AuthorizationMenu(
    onLogin: (username: String, password: String, onSuccess: (LoginState) -> Unit) -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoggingIn by rememberSaveable { mutableStateOf(false) }
    val passwordFieldFocusRequester = remember { FocusRequester() }

    val canLogin by remember { derivedStateOf { username.isNotBlank() && password.isNotBlank() } }

    val focusManager = LocalFocusManager.current

    val login = {
        if (canLogin) {
            isLoggingIn = true
            focusManager.clearFocus()
            onLogin(username, password) {
                isLoggingIn = false
                if (it is LoginState.Authorized) {
                    username = ""
                    password = ""
                }
            }
        }
    }


    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = {
            Text(stringResource(R.string.login_username))
        },
        singleLine = true,
        enabled = !isLoggingIn,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.None
        ),
        keyboardActions = KeyboardActions { passwordFieldFocusRequester.requestFocus() }
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        visualTransformation = PasswordVisualTransformation(),
        label = {
            Text(stringResource(R.string.login_password))
        },
        singleLine = true,
        enabled = !isLoggingIn,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(passwordFieldFocusRequester),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions { login() }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = {
            login()
        },
        enabled = canLogin && !isLoggingIn,
        modifier = Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            Modifier.heightIn(min = ButtonDefaults.MinHeight)
        ) {
            Row(
                modifier = Modifier
                    .padding(ButtonDefaults.ContentPadding)
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.login_login))
            }
            if (isLoggingIn) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Preview
@Composable
fun PreviewAuthorizationMenu() {
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = BASE_PADDING_HORIZONTAL)
            .fillMaxSize()
    ) {
        AuthorizationMenu { _, _, cb ->
            scope.launch {
                delay(5000L)
                cb(LoginState.Authorized("test", 0))
            }
        }
    }
}