package ru.herobrine1st.e621

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.herobrine1st.e621.api.Order
import ru.herobrine1st.e621.api.Rating
import ru.herobrine1st.e621.ui.*
import ru.herobrine1st.e621.ui.theme.E621Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            E621Theme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val screen by remember { derivedStateOf { Screens.byRoute[navBackStackEntry?.destination?.route] } }
                LaunchedEffect(screen) {
                    Log.d("MainActivity", screen?.name ?: "No screen")
                }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(stringResource(screen?.title ?: R.string.app_name))
                            },
                            backgroundColor = MaterialTheme.colors.primarySurface,
                            elevation = 12.dp,
                            actions = { screen?.appBarActions?.let { it(navController) } }
                        )
                    }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Screens.Home.route
                        ) {
                            composable(Screens.Home.route) {
                                Home(navController)
                            }
                            composable(Screens.Search.route, Screens.Search.arguments) { entry ->
                                val arguments: Bundle =
                                    entry.arguments!!
                                val searchOptions = remember {
                                    SearchOptions(
                                        arguments.getString("tags")!!.let { if(it.isBlank()) emptyList() else it.split(",") },
                                        Order.valueOf(arguments.getString("order")!!),
                                        arguments.getBoolean("ascending"),
                                        Rating.valueOf(arguments.getString("rating")!!)
                                    )
                                }
                                Search(searchOptions) {
                                    navController.navigate(
                                        Screens.Posts.buildRoute {
                                            addArgument("tags", it.tags.joinToString(","))
                                            addArgument("order", it.order.name)
                                            addArgument("ascending", it.orderAscending)
                                            addArgument("rating", it.rating.name)
                                        }
                                    ) {
                                        navController.popBackStack()
                                    }
                                }
                            }
                            composable(Screens.Posts.route, Screens.Posts.arguments) {
                                val arguments =
                                    it.arguments!! // Видимо если без аргументов вызвано - тогда null, ну в таком случае ошибка будет в другом месте
                                val tags = arguments.getString("tags")!!
                                val order = arguments.getString("order")!!
                                val ascending = arguments.getBoolean("ascending")
                                val rating = arguments.getString("rating")!!
                                Posts(
                                    query = tags,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}