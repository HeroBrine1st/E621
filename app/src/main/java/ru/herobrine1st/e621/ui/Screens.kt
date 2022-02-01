package ru.herobrine1st.e621.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import ru.herobrine1st.e621.R
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class RouteBuilder constructor(
    private val initialRoute: String,
    private val initialArguments: Array<out String>
) {
    private val arguments: MutableMap<String, String> = HashMap()
    fun addArgument(key: String, value: Any, encode: Boolean = false) {
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
    val initialRoute: String,
    private vararg val arguments: String
) {
    Home(R.string.app_name, Icons.Default.Home, "main"),
    Search(
        R.string.search,
        Icons.Default.Search,
        "search",
        "tags", "order", "ascending", "rating"
    ),
    Posts(
        R.string.posts,
        Icons.Default.Feed,
        "posts",
        "tags", "order", "ascending", "rating" // TODO переделать на navArgument
    );

    val route: String
        get() {
            if (arguments.isEmpty()) return initialRoute
            return initialRoute + arguments.joinToString(
                separator = "&",
                prefix = "?"
            ) {
                return@joinToString "$it={$it}"
            }
        }

    fun buildRoute(builder: RouteBuilder.() -> Unit): String {
        return RouteBuilder(initialRoute, arguments).apply(builder).build()
    }
}
