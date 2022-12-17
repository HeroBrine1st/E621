/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.component.BASE_PADDING_HORIZONTAL
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.screen.home.HomeViewModel.LoginState

@Composable
fun Home(
    navigateToSearch: () -> Unit,
    navigateToFavorites: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    Base {
        Button(
            onClick = navigateToSearch,
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            Text(stringResource(R.string.search))
            Icon(Icons.Rounded.NavigateNext, contentDescription = null)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Crossfade(targetState = viewModel.state) { state ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state) {
                    LoginState.LOADING -> CircularProgressIndicator()
                    LoginState.AUTHORIZED -> {
                        Button(
                            onClick = {
                                viewModel.logout()
                            },
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.login_logout))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = navigateToFavorites,
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.favourites))
                        }
                    }
                    LoginState.NO_AUTH, LoginState.UNKNOWN_API_ERROR -> AuthorizationMenu { u, p, cb ->
                        viewModel.login(u, p, cb)
                    }
                    LoginState.IO_ERROR -> {
                        Text(stringResource(R.string.network_error))
                        Button(onClick = { viewModel.checkAuthorization() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    LoginState.INTERNAL_SERVER_ERROR -> {
                        Text(stringResource(R.string.internal_server_error))
                        Button(onClick = { viewModel.checkAuthorization() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    LoginState.API_TEMPORARILY_UNAVAILABLE -> {
                        Text(stringResource(R.string.api_temporarily_unavailable))
                        Button(onClick = { viewModel.checkAuthorization() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }
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
                if (it == LoginState.AUTHORIZED) {
                    username = ""
                    password = ""
                }
            }
        }
    }


    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text(stringResource(R.string.login_username)) },
        singleLine = true,
        enabled = !isLoggingIn,
        modifier = Modifier
            .fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions { passwordFieldFocusRequester.requestFocus() }
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        visualTransformation = PasswordVisualTransformation(),
        onValueChange = { password = it },
        label = { Text(stringResource(R.string.login_password)) },
        singleLine = true,
        enabled = !isLoggingIn,
        modifier = Modifier
            .focusRequester(passwordFieldFocusRequester)
            .fillMaxWidth(),
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
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = BASE_PADDING_HORIZONTAL)
            .fillMaxSize()
    ) {
        AuthorizationMenu { _, _, cb ->
            scope.launch {
                delay(5000L)
                cb(LoginState.AUTHORIZED)
            }
        }
    }
}