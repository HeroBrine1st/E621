package ru.herobrine1st.e621.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.Order
import ru.herobrine1st.e621.api.Rating
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class RouteBuilder(
    private val initialRoute: String,
    private val initialArguments: Collection<String>
) {
    private val arguments: MutableMap<String, String> = HashMap()
    fun addArgument(key: String, value: Any?, encode: Boolean = false) {
        assert(key in initialArguments) { "Invalid argument key" }
        if (encode)
            arguments[key] = URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.toString())
        else
            arguments[key] = value.toString()
    }

    fun build(): String = initialRoute + arguments.map { "${it.key}=${it.value}" }
        .joinToString("&", prefix = "?")
}

enum class Screens(
    @StringRes val title: Int,
    val icon: ImageVector,
    private val initialRoute: String,
    vararg arguments: NamedNavArgument,
    val appBarActions: @Composable RowScope.(navHostController: NavHostController) -> Unit = {}
) {
    Home(R.string.app_name, Icons.Default.Home, "main", appBarActions = HomeAppBarActions),
    Search(
        R.string.search,
        Icons.Default.Search,
        "search",
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
            defaultValue = Rating.values().joinToString(",") { it.name }
        },
    ),
    Posts(
        R.string.posts,
        Icons.Default.Feed,
        "posts",
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
            defaultValue = Rating.values().joinToString(",") { it.name }
        },
        appBarActions = PostsAppBarActions
    ),
    Settings(R.string.settings, Icons.Default.Settings, "settings");

    companion object {
        val byRoute: Map<String, Screens> = HashMap<String, Screens>().apply {
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
