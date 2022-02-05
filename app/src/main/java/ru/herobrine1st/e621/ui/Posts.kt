package ru.herobrine1st.e621.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.util.lateinitMutableState

private var vm: PostsViewModel by lateinitMutableState()

class PostsViewModel: ViewModel() {

}

val PostsAppBarActions: @Composable RowScope.(NavHostController) -> Unit = { navController ->
    IconButton(onClick = {
        val arguments = navController.currentBackStackEntry!!.arguments!!
        navController.navigate(
            Screens.Search.buildRoute {
                addArgument("tags", arguments.getString("tags"))
                addArgument("order", arguments.getString("order"))
                addArgument("ascending", arguments.getBoolean("ascending"))
                addArgument("rating", arguments.getString("rating"))
            }
        )
    }) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(R.string.search)
        )
    }
}

@Composable
fun Posts(query: List<String>, navController: NavHostController) {
    vm = viewModel()
    Base {
        Button(onClick = {
            navController.navigate(Screens.Home.route)
        }) {
            Text(query.joinToString(","))
        }
    }
}