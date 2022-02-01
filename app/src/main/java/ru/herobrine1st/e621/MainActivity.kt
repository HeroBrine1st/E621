package ru.herobrine1st.e621

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.herobrine1st.e621.api.Order
import ru.herobrine1st.e621.api.Rating
import ru.herobrine1st.e621.ui.Search
import ru.herobrine1st.e621.ui.Posts
import ru.herobrine1st.e621.ui.Screens
import ru.herobrine1st.e621.ui.component.Base
import ru.herobrine1st.e621.ui.theme.E621Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            E621Theme {
                val navController = rememberNavController()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(stringResource(R.string.app_name))
                            },
                            backgroundColor = MaterialTheme.colors.primarySurface,
                            elevation = 12.dp
                        )
                    }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {


                        NavHost(
                            navController = navController,
                            startDestination = Screens.Home.initialRoute
                        ) {
                            composable(Screens.Home.initialRoute) {
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
                                }
                            }
                            composable(Screens.Search.route,
                                arguments = listOf(
                                    navArgument("tags") {
                                        type = NavType.StringType
                                        defaultValue = ""
                                    },
                                    navArgument("order") {
                                        type = NavType.StringType
                                        defaultValue = Order.NEWEST_TO_OLDEST.name
                                    },
                                    navArgument("ascending") {
                                        type = NavType.BoolType
                                        defaultValue = false
                                    },
                                    navArgument("rating") {
                                        type = NavType.StringType
                                        defaultValue = Rating.ANY.name
                                    }
                                )
                            ) {
                                Search {
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
                            composable(Screens.Posts.route,
                                arguments = listOf(
                                    navArgument("tags") {
                                        type = NavType.StringType
                                        defaultValue = ""
                                    },
                                    navArgument("order") {
                                        type = NavType.StringType
                                        defaultValue = Order.NEWEST_TO_OLDEST.name
                                    },
                                    navArgument("ascending") {
                                        type = NavType.BoolType
                                        defaultValue = false
                                    },
                                    navArgument("rating") {
                                        type = NavType.StringType
                                        defaultValue = Rating.ANY.name
                                    }
                                )
                            ) {
                                val query = it.arguments?.getString("tags")
                                query?.let {
                                    Posts(
                                        query = query
                                    )
                                } ?: Text("Arguments field is null")
                            }
                        }
                    }
                }
            }
        }
    }
}