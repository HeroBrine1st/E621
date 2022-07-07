package ru.herobrine1st.e621.ui

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.compose.currentBackStackEntryAsState
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.screen.Screen

@Composable
fun MainScaffold(navController: NavHostController, scaffoldState: ScaffoldState, onOpenBlacklistDialog: () -> Unit) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val screen by remember { derivedStateOf { Screen.byRoute[navBackStackEntry?.destination?.route] } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(screen?.title ?: R.string.app_name))
                },
                backgroundColor = MaterialTheme.colors.primarySurface,
                elevation = 12.dp,
                actions = {
                    val saveableStateHolder = rememberSaveableStateHolder()
                    navBackStackEntry?.LocalOwnersProvider(saveableStateHolder = saveableStateHolder) {
                        screen?.appBarActions?.invoke(
                            this,
                            navController
                        )
                    }
                    ActionBarMenu(navController, onOpenBlacklistDialog)
                }
            )
        },
        scaffoldState = scaffoldState,
        floatingActionButton = {
            val saveableStateHolder = rememberSaveableStateHolder()
            navBackStackEntry?.LocalOwnersProvider(saveableStateHolder = saveableStateHolder) {
                screen?.floatingActionButton?.invoke()
            }
        }
    ) {
        Surface(
            color = MaterialTheme.colors.background
        ) {
            Navigator(navController)
        }
    }
}