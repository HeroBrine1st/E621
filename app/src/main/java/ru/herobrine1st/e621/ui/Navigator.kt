package ru.herobrine1st.e621.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.preference.getPreferencesAsState
import ru.herobrine1st.e621.ui.screen.Screen
import ru.herobrine1st.e621.ui.screen.home.Home
import ru.herobrine1st.e621.ui.screen.posts.Post
import ru.herobrine1st.e621.ui.screen.posts.Posts
import ru.herobrine1st.e621.ui.screen.search.Search
import ru.herobrine1st.e621.ui.screen.settings.Settings
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklist
import ru.herobrine1st.e621.util.FavouritesSearchOptions
import ru.herobrine1st.e621.util.PostsSearchOptions
import javax.inject.Inject

@Composable
fun Navigator(navController: NavHostController, viewModel: NavigatorViewModel = hiltViewModel()) {
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
                    navController.navigate(
                        Screen.Post.buildRoute {
                            addArgument("post", post)
                            addArgument("scrollToComments", scrollToComments)
                            addArgument("query", searchOptions)
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
            val username by viewModel.usernameFlow.collectAsState(initial = null) // I think it is the moment when authorization data should live in DataStore..

            Posts(
                searchOptions,
                isBlacklistEnabled = preferences.blacklistEnabled,
                openPost = { post, scrollToComments ->
                    navController.navigate(
                        Screen.Post.buildRoute {
                            addArgument("post", post)
                            addArgument("scrollToComments", scrollToComments)
                            addArgument(
                                "query", PostsSearchOptions(
                                    favouritesOf = arguments.getString("user") ?: username
                                )
                            )
                        }
                    )
                }
            )
        }
        composable(Screen.Post.route, Screen.Post.arguments) {
            val arguments =
                it.arguments!!
            Post(
                arguments.getParcelable("post")!!,
                arguments.getBoolean("scrollToComments"),
                arguments.getParcelable("query")!!,
                onModificationClick = {
                    navController.navigate(
                        Screen.Search.buildRoute {
                            addArgument("query", it)
                        }
                    )
                },
                onExit = { navController.popBackStack() }
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

@HiltViewModel
class NavigatorViewModel @Inject constructor(
    val authorizationRepository: AuthorizationRepository
) : ViewModel() {
    val usernameFlow = authorizationRepository.getAccountFlow().map { it?.login }
}