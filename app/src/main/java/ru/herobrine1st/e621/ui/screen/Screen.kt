/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.ui.screen

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.*
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.api.PostsSearchOptionsNavType
import ru.herobrine1st.e621.ui.screen.favourites.FavouritesAppBarActions
import ru.herobrine1st.e621.ui.screen.posts.PostsAppBarActions
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklistAppBarActions
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklistFloatingActionButton
import ru.herobrine1st.e621.util.JsonSerializable
import ru.herobrine1st.e621.util.debug
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class RouteBuilder(
    private val initialRoute: String,
    private val initialArguments: Collection<String>
) {
    private val arguments: MutableMap<String, String> = HashMap()
    fun addArgument(key: String, value: Any?, encode: Boolean = false) {
        debug {
            assert(key in initialArguments) { "Invalid argument key" }
            Log.d("RouteBuilder", "Adding argument $key=$value to route $initialRoute")
        }
        if (value == null) return
        if (encode)
            arguments[key] = URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.toString())
        else
            arguments[key] = value.toString()
    }

    fun addArgument(key: String, value: JsonSerializable) {
        addArgument(key, value.serializeToJson(), true)
    }

    fun build(): String = initialRoute + arguments.map { "${it.key}=${it.value}" }
        .joinToString("&", prefix = "?")
}

enum class Screen(
    @StringRes val title: Int,
    val icon: ImageVector,
    private val initialRoute: String,
    vararg arguments: NamedNavArgument,
    val appBarActions: @Composable RowScope.(NavHostController) -> Unit = {},
    val floatingActionButton: @Composable () -> Unit = {},
    val deepLinks: List<NavDeepLink> = listOf()
) {
    Home(R.string.app_name, Icons.Default.Home, "main"),
    Search(
        R.string.search,
        Icons.Default.Search,
        "search",
        navArgument("query") {
            type = PostsSearchOptionsNavType()
            defaultValue = PostsSearchOptions.DEFAULT
        },
    ),
    Posts(
        R.string.posts,
        Icons.Default.Feed,
        "posts",
        navArgument("query") {
            type = PostsSearchOptionsNavType()
        },
        appBarActions = { PostsAppBarActions(it) }
    ),
    Post(
        R.string.post, Icons.Default.Feed, "post",
        navArgument("id") {
            type = NavType.IntType
        },
//        navArgument("post") {
//            type = PostNavType()
//            nullable = true
//            defaultValue = null
//        },
        navArgument("openComments") {
            type = NavType.BoolType
            defaultValue = false
        },
//        navArgument("query") {
//            type = PostsSearchOptionsNavType()
//            nullable = true
//            defaultValue = null
//        },
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "${BuildConfig.DEEP_LINK_BASE_URL}/posts/{id}"
            }
        )
    ),
    Favourites(
        R.string.favourites,
        Icons.Default.Feed,
        "favourites",
        navArgument("user") {
            type = NavType.StringType
            nullable = true
        },
        appBarActions = { FavouritesAppBarActions(it) }
    ),
    Settings(R.string.settings, Icons.Default.Settings, "settings"),
    SettingsBlacklist(
        R.string.blacklist,
        Icons.Default.Block,
        "settings/blacklist",
        appBarActions = { SettingsBlacklistAppBarActions() },
        floatingActionButton = { SettingsBlacklistFloatingActionButton() }),
    SettingsAbout(R.string.about, Icons.Default.Copyright, "settings/about"),
    License(R.string.license_name, Icons.Default.Info, "settings/license");

    companion object {
        val byRoute: Map<String, Screen> = HashMap<String, Screen>().apply {
            for (value in values()) {
                put(value.route, value)
            }
        }
    }

    val arguments = listOf(*arguments)
    val route: String =
        if (arguments.isEmpty()) initialRoute
        else initialRoute + arguments.map { it.name }.joinToString(
            separator = "&",
            prefix = "?"
        ) {
            "$it={$it}"
        }

    fun buildRoute(builder: RouteBuilder.() -> Unit): String {
        return RouteBuilder(initialRoute, arguments.map { it.name }).apply(builder).build()
    }
}