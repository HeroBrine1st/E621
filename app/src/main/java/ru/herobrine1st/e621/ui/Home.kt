package ru.herobrine1st.e621.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ru.herobrine1st.e621.R
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
private var vm: HomeViewModel by lateinitMutableState()

class HomeViewModel : ViewModel() {
    var username by mutableStateOf("")
}


val HomeAppBarActions: @Composable RowScope.(NavHostController) -> Unit = { _ ->
    Text(vm.username)
    Button(onClick = {
        vm.username += "1"
    }) {
        Text("Button")
    }
}


@Composable
fun Home(navController: NavHostController) {
    vm = viewModel()
    Base {
        Button(
            onClick = {
                navController.navigate("search")
            },
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            Text(stringResource(R.string.search))
            Icon(Icons.Rounded.NavigateNext, contentDescription = null)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = vm.username,
            onValueChange = { vm.username = it },
            label = { Text("username") },
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}