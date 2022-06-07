package ru.herobrine1st.e621.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.enumeration.AuthState
import ru.herobrine1st.e621.ui.component.Base

@Composable
fun Home(
    authState: AuthState,
    onLogout: () -> Unit,
    onLogin: (username: String, password: String, onSuccess: () -> Unit) -> Unit,
    navigateToSearch: () -> Unit,
    navigateToFavorites: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

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
        when (authState) {
            AuthState.LOADING -> {
                CircularProgressIndicator()
            }
            AuthState.AUTHORIZED -> {
                Button(
                    onClick = {
                        onLogout()
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
            else -> {
                when (authState) {
                    AuthState.UNAUTHORIZED -> {
                        Text(stringResource(R.string.login_unauthorized))
                    }
                    AuthState.IO_ERROR -> {
                        Text(stringResource(R.string.network_error))
                    }
                    AuthState.SQL_ERROR -> {
                        Text(stringResource(R.string.database_error))
                    }
                    else -> {}
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.login_username)) },
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.login_password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onLogin(username, password) {
                            username = ""
                            password = ""
                        }
                    },
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    Text(stringResource(R.string.login_login))
                }
            }
        }
    }
}