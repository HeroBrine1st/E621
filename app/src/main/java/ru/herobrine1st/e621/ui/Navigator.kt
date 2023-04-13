/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.herobrine1st.e621.ui

import android.os.Bundle
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.herobrine1st.e621.api.FavouritesSearchOptions
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.ui.screen.Screen
import ru.herobrine1st.e621.ui.screen.home.Home
import ru.herobrine1st.e621.ui.screen.post.Post
import ru.herobrine1st.e621.ui.screen.posts.Posts
import ru.herobrine1st.e621.ui.screen.search.Search
import ru.herobrine1st.e621.ui.screen.settings.*
import ru.herobrine1st.e621.util.getParcelableCompat

@Composable
fun Navigator(navController: NavHostController, snackbarHostState: SnackbarHostState) {
    val preferences = LocalPreferences.current

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            MainScaffold(
                navController = navController,
                scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
                screen = Screen.Home
            ) {
                Home(
                    navigateToFavorites = {
                        navController.navigate(Screen.Favourites.route)
                    },
                    navigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    }
                )
            }
        }
        composable(
            Screen.Search.route,
            Screen.Search.arguments
        ) { entry ->
            val arguments: Bundle =
                entry.arguments!!

            val searchOptions = arguments.getParcelableCompat("query")
                ?: PostsSearchOptions.DEFAULT
            MainScaffold(
                navController = navController,
                scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
                screen = Screen.Search
            ) {
                Search(searchOptions) {
                    navController.popBackStack()
                    navController.navigate(
                        Screen.Posts.buildRoute {
                            addArgument("query", it)
                        }
                    )
                }
            }
        }
        composable(Screen.Posts.route, Screen.Posts.arguments) {
            val searchOptions = remember {
                it.arguments!!.getParcelableCompat<PostsSearchOptions>("query")!!
            }

            MainScaffold(
                navController = navController,
                scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
                screen = Screen.Posts
            ) {
                Posts(
                    searchOptions,
                    preferences.hasAuth(),
                    openPost = { post, scrollToComments ->
                        navController.currentBackStackEntry!!.savedStateHandle["clickedPost"] = post
                        navController.currentBackStackEntry!!.savedStateHandle["query"] =
                            searchOptions
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
        }
        composable(Screen.Favourites.route, Screen.Favourites.arguments) {
            val arguments =
                it.arguments!!
            val searchOptions =
                remember { FavouritesSearchOptions(arguments.getString("user")) }
            val username by remember { derivedStateOf { if (preferences.hasAuth()) preferences.auth.username else null } }
            MainScaffold(
                navController = navController,
                scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
                screen = Screen.Favourites
            ) {
                Posts(
                    searchOptions,
                    preferences.hasAuth(),
                    openPost = { post, scrollToComments ->
                        navController.currentBackStackEntry!!.savedStateHandle["clickedPost"] = post
                        navController.currentBackStackEntry!!.savedStateHandle["query"] =
                            PostsSearchOptions(
                                favouritesOf = arguments.getString("user") ?: username
                            )
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
        }
        composable(Screen.Post.route, Screen.Post.arguments, deepLinks = Screen.Post.deepLinks) {
            val arguments =
                it.arguments!!

            MainScaffold(
                navController = navController,
                scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
                screen = Screen.Post
            ) {
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
        }
        composable(Screen.Settings.route) {
            MainScaffold(
                navController = navController,
                scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
                screen = Screen.Settings
            ) {
                Settings(navController)
            }
        }
        composable(Screen.SettingsBlacklist.route) {
            MainScaffold(
                navController = navController,
                scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
                screen = Screen.SettingsBlacklist
            ) {
                SettingsBlacklist {
                    navController.popBackStack()
                }
            }
        }
        composable(Screen.SettingsAbout.route) {
            MainScaffold(
                navController = navController,
                scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
                screen = Screen.SettingsAbout
            ) {
                SettingsAbout(navigateToLicense = {
                    navController.navigate(Screen.SettingsLicense.route)
                }, navigateToOssLicenses = {
                    navController.navigate(Screen.SettingsLicenses.route)
                })
            }
        }
        composable(Screen.SettingsLicense.route) {
            MainScaffold(
                navController = navController,
                scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
                screen = Screen.SettingsLicense
            ) {
                SettingsLicense()
            }
        }
        composable(Screen.SettingsLicenses.route) {
            MainScaffold(
                navController = navController,
                scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState),
                screen = Screen.SettingsLicenses
            ) {
                SettingsLicenses()
            }
        }
    }
}