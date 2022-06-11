package ru.herobrine1st.e621.ui.screen

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.screen.favourites.FavouritesAppBarActions
import ru.herobrine1st.e621.ui.screen.posts.PostNavType
import ru.herobrine1st.e621.ui.screen.posts.PostsAppBarActions
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklistAppBarActions
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklistFloatingActionButton
import ru.herobrine1st.e621.util.JsonSerializable
import ru.herobrine1st.e621.util.PostsSearchOptions
import ru.herobrine1st.e621.util.PostsSearchOptionsNavType
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
    val appBarActions: @Composable RowScope.(NavHostController, ApplicationViewModel) -> Unit = { _, _ -> },
    val floatingActionButton: @Composable (ApplicationViewModel) -> Unit = {}
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
        appBarActions = { it, _ -> PostsAppBarActions(it) }
    ),
    Post(R.string.post, Icons.Default.Feed, "post",
        navArgument("post") {
            type = PostNavType()
        },
        navArgument("scrollToComments") {
            type = NavType.BoolType
            defaultValue = false
        }),
    Favourites(
        R.string.favourites,
        Icons.Default.Feed,
        "favourites",
        navArgument("user") {
            type = NavType.StringType
            nullable = true
        },
        appBarActions = { it, it1 -> FavouritesAppBarActions(it, it1) }
    ),
    Settings(R.string.settings, Icons.Default.Settings, "settings"),
    SettingsBlacklist(
        R.string.blacklist,
        Icons.Default.Block,
        "settings/blacklist",
        appBarActions = { _, _ -> SettingsBlacklistAppBarActions() },
        floatingActionButton = { SettingsBlacklistFloatingActionButton() });

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
            return@joinToString "$it={$it}"
        }

    fun buildRoute(builder: RouteBuilder.() -> Unit): String {
        return RouteBuilder(initialRoute, arguments.map { it.name }).apply(builder).build()
    }
}