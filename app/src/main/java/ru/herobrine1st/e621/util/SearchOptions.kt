package ru.herobrine1st.e621.util

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.api.Order
import ru.herobrine1st.e621.api.Rating

// TODO move to NavType and JSON in arguments - this way Jackson will manage all the stuff
// class PostsSearchOptionsNavType: NavType<PostsSearchOptions>(false) {
//     override fun get(bundle: Bundle, key: String): PostsSearchOptions? {
//         // Get from bundle
//     }
//
//     override fun parseValue(value: String): PostsSearchOptions {
//         // Parse json
//     }
//
//     override fun put(bundle: Bundle, key: String, value: PostsSearchOptions) {
//         // Build bundle
//     }
// }

interface SearchOptions {
    val tags: List<String>
    val order: Order?
    val orderAscending: Boolean
    val rating: List<Rating>
    val favouritesOf: String? // "favorited_by" in api

    suspend fun prepareRequestUrl(api: Api): HttpUrl
}

data class PostsSearchOptions(
    override val tags: List<String>,
    override val order: Order,
    override val orderAscending: Boolean,
    override val rating: List<Rating>,
    override val favouritesOf: String?,
) : SearchOptions {
    constructor(bundle: Bundle) : this(
        bundle.getString("tags")!!
            .let { if (it.isBlank()) emptyList() else it.split(",") },
        Order.valueOf(bundle.getString("order")!!),
        bundle.getBoolean("ascending"),
        bundle.getString("rating")!!.split(",").filter { it.isNotBlank() }
            .map { Rating.valueOf(it) },
        bundle.getString("fav")
    )

    private fun compileToQuery(): String {
        var query = tags.joinToString(" ")
        (if (orderAscending) this.order.ascendingApiName else this.order.apiName)?.let {
            query += " order:$it"
        }
        if (rating.size < Rating.values().size && rating.isNotEmpty()) {
            query += " " + if (rating.size == 1) {
                "rating:${rating[0].apiName}"
            } else {
                rating.joinToString(" ") { "~rating:${it.apiName}" }
            }
        }
        if (favouritesOf != null) {
            query += " fav:$favouritesOf"
        }
        return query
    }

    override suspend fun prepareRequestUrl(api: Api): HttpUrl {
        return Api.preparePostsRequestUrl(compileToQuery())
    }
}

data class FavouritesSearchOptions(override val favouritesOf: String?) : SearchOptions {
    override val tags: List<String> = emptyList()
    override val order: Order? = null
    override val orderAscending: Boolean = false
    override val rating: List<Rating> = emptyList()

    override suspend fun prepareRequestUrl(api: Api): HttpUrl {
        val id = if (favouritesOf != null) withContext(Dispatchers.IO) {
            api.getIdOfUser(favouritesOf)
        } else null
        return Api.prepareFavouritesRequestUrl(id)
    }
}