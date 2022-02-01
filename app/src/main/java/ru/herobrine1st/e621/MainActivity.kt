package ru.herobrine1st.e621

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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
import ru.herobrine1st.e621.ui.theme.E621Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            E621Theme {
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
                        val navController = rememberNavController()

                        NavHost(navController = navController, startDestination = "search") {
                            composable("search") {
                                Search {
                                    navController.navigate("posts?tags=${it.tags.joinToString(" ")}") {
                                        navController.popBackStack()
                                    }
                                }
                            }
                            composable(
                                "posts?tags={tags}",
                                arguments = listOf(
                                    navArgument("tags") {
                                        type = NavType.StringArrayType
                                        defaultValue = "tags"
                                    },
                                    navArgument("order") {
                                        type = NavType.EnumType(Order::class.java)
                                        defaultValue = Order.NEWEST_TO_OLDEST.name
                                    },
                                    navArgument("ascending") {
                                        type = NavType.BoolType
                                        defaultValue = "false"
                                    },
                                    navArgument("rating") {
                                        type = NavType.EnumType(Order::class.java)
                                        defaultValue = Rating.ANY.name
                                    }
                                )
                            ) {
                                val query = it.arguments?.getString("tags")
                                query?.let {
                                    Posts(
                                        navController = navController,
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