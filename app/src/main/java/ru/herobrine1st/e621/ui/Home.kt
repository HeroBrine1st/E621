package ru.herobrine1st.e621.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.AuthState
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.entity.Auth
import ru.herobrine1st.e621.ui.component.Base

/**
 * Минусы:
 *
 * - Глобальная переменная
 *
 * Плюсы:
 *
 * - DRY
 * - KISS
 * - Не синглтон
 *
 * Других способов поднять произвольную viewModel в AppBar нет. Возможно, здесь будет полезна библиотека Hilt.
 *
 */
//private var vm: HomeViewModel by lateinitMutableState()
//
//class HomeViewModel : ViewModel() {
//
//}


val HomeAppBarActions: @Composable RowScope.(NavHostController) -> Unit = { _ ->

}

@Composable
fun Home(navController: NavHostController, applicationViewModel: ApplicationViewModel) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val authState = applicationViewModel.authState

    Base(Alignment.CenterHorizontally) {
        Button(
            onClick = {
                navController.navigate(Screens.Search.route)
            },
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
                        applicationViewModel.logout()
                    },
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    Text("Logout")
                }
            }
            else -> {
                when (authState) {
                    AuthState.UNAUTHORIZED -> {
                        Text("Invalid login/apiKey")
                    }
                    AuthState.IO_ERROR -> {
                        Text("IO Error")
                    }
                    AuthState.SQL_ERROR -> {
                        Text("SQL Error")
                    }
                    else -> {}
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        applicationViewModel.tryAuthenticate(username, password) {
                            username = ""
                            password = ""
                        }
                    },
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                ) {
                    Text("Login")
                }
            }
        }
    }
}