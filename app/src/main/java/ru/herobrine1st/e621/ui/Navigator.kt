package ru.herobrine1st.e621.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.herobrine1st.e621.preference.getPreferencesAsState
import ru.herobrine1st.e621.ui.screen.Screen
import ru.herobrine1st.e621.ui.screen.home.Home
import ru.herobrine1st.e621.ui.screen.post.Post
import ru.herobrine1st.e621.ui.screen.posts.Posts
import ru.herobrine1st.e621.ui.screen.search.Search
import ru.herobrine1st.e621.ui.screen.settings.Settings
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklist
import ru.herobrine1st.e621.util.FavouritesSearchOptions
import ru.herobrine1st.e621.util.PostsSearchOptions

@Composable
fun Navigator(navController: NavHostController) {
    val context = LocalContext.current

    val preferences by context.getPreferencesAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            Home(
                navigateToFavorites = {
                    navController.navigate(Screen.Favourites.route)
                },
                navigateToSearch = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }
        composable(
            Screen.Search.route,
            Screen.Search.arguments
        ) { entry ->
            val arguments: Bundle =
                entry.arguments!!

            val searchOptions = arguments.getParcelable("query")
                ?: PostsSearchOptions.DEFAULT
            Search(searchOptions) {
                navController.popBackStack()
                navController.navigate(
                    Screen.Posts.buildRoute {
                        addArgument("query", it)
                    }
                )
            }
        }
        composable(Screen.Posts.route, Screen.Posts.arguments) {
            val searchOptions = remember {
                it.arguments!!.getParcelable<PostsSearchOptions>("query")!!
            }

            Posts(
                searchOptions,
                isBlacklistEnabled = preferences.blacklistEnabled,
                openPost = { post, scrollToComments ->
                    navController.currentBackStackEntry!!.savedStateHandle["clickedPost"] = post
                    navController.currentBackStackEntry!!.savedStateHandle["query"] = searchOptions
                    navController.navigate(
                        Screen.Post.buildRoute {
                            addArgument("id", post.id)
//                            addArgument("post", post)
                            addArgument("openComments", scrollToComments)
//                            addArgument("query", searchOptions)
                        }
                    )
                }
            )
        }
        composable(Screen.Favourites.route, Screen.Favourites.arguments) {
            val arguments =
                it.arguments!!
            val searchOptions =
                remember { FavouritesSearchOptions(arguments.getString("user")) }
            val username by remember { derivedStateOf { if (preferences.hasAuth()) preferences.auth.username else null } }
            Posts(
                searchOptions,
                isBlacklistEnabled = preferences.blacklistEnabled,
                openPost = { post, scrollToComments ->
                    navController.currentBackStackEntry!!.savedStateHandle["clickedPost"] = post
                    navController.currentBackStackEntry!!.savedStateHandle["query"] =
                        PostsSearchOptions(favouritesOf = arguments.getString("user") ?: username)
                    navController.navigate(
                        Screen.Post.buildRoute {
                            addArgument("id", post.id)
//                            addArgument("post", post)
                            addArgument("openComments", scrollToComments)
//                            addArgument(
//                                "query", PostsSearchOptions(
//                                    favouritesOf = arguments.getString("user") ?: username
//                                )
//                            )
                        }
                    )
                }
            )
        }
        composable(Screen.Post.route, Screen.Post.arguments, deepLinks = Screen.Post.deepLinks) {
            val arguments =
                it.arguments!!

            Post(
                arguments.getInt("id"),
//                arguments.getParcelable("post"),
                navController.previousBackStackEntry?.savedStateHandle?.get("clickedPost"),
                arguments.getBoolean("openComments"),
//                arguments.getParcelable("query")
                navController.previousBackStackEntry?.savedStateHandle?.get("query")
                    ?: PostsSearchOptions.DEFAULT,
                onModificationClick = {
                    navController.navigate(
                        Screen.Search.buildRoute {
                            addArgument("query", it)
                        }
                    )
                }
            )
        }
        composable(Screen.Settings.route) {
            Settings(navController)
        }
        composable(Screen.SettingsBlacklist.route) {
            SettingsBlacklist {
                navController.popBackStack()
            }
        }
    }
}