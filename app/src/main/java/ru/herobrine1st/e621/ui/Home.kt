package ru.herobrine1st.e621.ui

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.herobrine1st.e621.Database
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.entity.Auth
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.util.lateinitMutableState

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

sealed class TestEnum(val user: Auth?) {
    object LOADING: TestEnum(null)
    class AUTHORIZED(user: Auth): TestEnum(user)
    object UNAUTHORIZED: TestEnum(null)
}

@Composable
fun Home(navController: NavHostController, db: Database) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var updateAuthState by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val authState by produceState<TestEnum>(initialValue = TestEnum.LOADING, updateAuthState) {
        value = TestEnum.LOADING
        delay(500L)
        val auth = db.authDao().get()
        if (auth == null) {
            value = TestEnum.UNAUTHORIZED
        } else {
            username = auth.login
            password = auth.apiKey

            value = TestEnum.AUTHORIZED(auth)
        }
    }

    Base {
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
        if (authState == TestEnum.LOADING) {
            CircularProgressIndicator()
        } else {
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
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if(authState == TestEnum.UNAUTHORIZED) {
                        coroutineScope.launch {
                            db.authDao().insert(Auth(username, password))
                            updateAuthState = !updateAuthState
                        }
                    } else {
                        val auth = authState.user!!
                        auth.apiKey = password
                        auth.login = username
                        coroutineScope.launch {
                            db.authDao().update(auth)
                            updateAuthState = !updateAuthState
                        }
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