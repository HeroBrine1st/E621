package ru.herobrine1st.e621

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import ru.herobrine1st.e621.ui.ActionBarMenu
import ru.herobrine1st.e621.ui.SnackbarController
import ru.herobrine1st.e621.ui.screen.Home
import ru.herobrine1st.e621.ui.screen.Posts
import ru.herobrine1st.e621.ui.screen.Screens
import ru.herobrine1st.e621.ui.screen.Search
import ru.herobrine1st.e621.ui.screen.settings.Settings
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklist
import ru.herobrine1st.e621.ui.theme.E621Theme
import ru.herobrine1st.e621.util.SearchOptions


class MainActivity : ComponentActivity() {
    companion object {
        val TAG = MainActivity::class.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db: Database = Room.databaseBuilder(
            applicationContext,
            Database::class.java, BuildConfig.DATABASE_NAME
        ).build()

        setContent {
            E621Theme(window) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val screen by remember { derivedStateOf { Screens.byRoute[navBackStackEntry?.destination?.route] } }
                val applicationViewModel: ApplicationViewModel =
                    viewModel(factory = ApplicationViewModel.Factory(db))
                val scaffoldState = rememberScaffoldState()

                LaunchedEffect(true) {
                    applicationViewModel.loadAllFromDatabase()
                }

                SnackbarController(applicationViewModel, scaffoldState)
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(stringResource(screen?.title ?: R.string.app_name))
                            },
                            backgroundColor = MaterialTheme.colors.primarySurface,
                            elevation = 12.dp,
                            actions = {
                                screen?.appBarActions?.invoke(
                                    this,
                                    navController,
                                    applicationViewModel
                                )
                                ActionBarMenu(navController, applicationViewModel)
                            }
                        )
                    },
                    scaffoldState = scaffoldState,
                    floatingActionButton = {
                        screen?.floatingActionButton?.invoke(applicationViewModel)
                    }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        CompositionLocalProvider(LocalDatabase provides db) {
                            NavHost(
                                navController = navController,
                                startDestination = Screens.Home.route
                            ) {
                                composable(Screens.Home.route) {
                                    Home(navController, applicationViewModel)
                                }
                                composable(
                                    Screens.Search.route,
                                    Screens.Search.arguments
                                ) { entry ->
                                    val arguments: Bundle =
                                        entry.arguments!!
                                    val searchOptions = remember { SearchOptions(arguments) }
                                    Search(searchOptions) {
                                        navController.popBackStack()
                                        navController.navigate(
                                            Screens.Posts.buildRoute {
                                                addArgument("tags", it.tags.joinToString(","))
                                                addArgument("order", it.order.name)
                                                addArgument("ascending", it.orderAscending)
                                                addArgument(
                                                    "rating",
                                                    it.rating.joinToString(",") { it.name })
                                            }
                                        )
                                    }
                                }
                                composable(Screens.Posts.route, Screens.Posts.arguments) {
                                    val arguments =
                                        it.arguments!! // Видимо если без аргументов вызвано - тогда null, ну в таком случае ошибка будет в другом месте
                                    val searchOptions = remember { SearchOptions(arguments) }
                                    Posts(searchOptions, applicationViewModel)
                                }
                                composable(Screens.Settings.route) {
                                    Settings(navController)
                                }
                                composable(Screens.SettingsBlacklist.route) {
                                    SettingsBlacklist(applicationViewModel) {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}