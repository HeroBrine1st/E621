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

package ru.herobrine1st.e621.ui.screen.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.home.HomeComponent
import ru.herobrine1st.e621.navigation.component.home.HomeComponent.LoginState
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffold
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffoldState

@Composable
fun Home(
    mainScaffoldState: MainScaffoldState,
    component: HomeComponent
) {
    MainScaffold(
        state = mainScaffoldState,
        title = { Text(stringResource(R.string.app_name)) }
    ) {
        Base {
            Button(
                onClick = component::navigateToSearch,
                colors = ButtonDefaults.filledTonalButtonColors(),
                elevation = ButtonDefaults.filledTonalButtonElevation(),
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.search))
                Icon(
                    Icons.Rounded.NavigateNext,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Crossfade(targetState = component.state) { state ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state) {
                        LoginState.Loading -> CircularProgressIndicator()
                        is LoginState.Authorized -> {
                            FilledTonalButton(
                                onClick = component::logout,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(text = stringResource(R.string.login_logout))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = component::navigateToFavourites,
                                modifier = Modifier
                                    .padding(4.dp)
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
                            Button(onClick = component::checkAuthorization) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                        LoginState.InternalServerError -> {
                            Text(stringResource(R.string.internal_server_error))
                            Button(
                                onClick = component::checkAuthorization
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                        LoginState.APITemporarilyUnavailable -> {
                            Text(stringResource(R.string.api_temporarily_unavailable))
                            Button(
                                onClick = component::checkAuthorization
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                        LoginState.UnknownAPIError -> {
                            Text(stringResource(R.string.unknown_api_error))
                            Row {
                                ElevatedButton(onClick = component::checkAuthorization) {
                                    Text(stringResource(R.string.retry))
                                }
                                Spacer(Modifier.size(8.dp))
                                FilledTonalButton(
                                    onClick = component::logout,
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = Color.Red
                                    )
                                ) {
                                    Text(stringResource(R.string.login_logout))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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
    OutlinedButton(
        onClick = {
            login()
        },
        enabled = canLogin && !isLoggingIn,
        modifier = Modifier
            .padding(4.dp)
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