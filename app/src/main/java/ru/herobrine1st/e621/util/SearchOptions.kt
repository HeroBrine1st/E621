package ru.herobrine1st.e621.util

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.navigation.NavType
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.parcelize.Parcelize
import ru.herobrine1st.e621.api.ApiException
import ru.herobrine1st.e621.api.IAPI
import ru.herobrine1st.e621.api.Order
import ru.herobrine1st.e621.api.Rating
import ru.herobrine1st.e621.api.model.Post
import java.io.IOException

interface SearchOptions {
    @Throws(ApiException::class, IOException::class)
    suspend fun getPosts(api: IAPI, limit: Int, page: Int): List<Post>
}

@Parcelize
data class PostsSearchOptions(
    val tags: List<String> = emptyList(),
    val order: Order = Order.NEWEST_TO_OLDEST,
    val orderAscending: Boolean = false,
    val rating: List<Rating> = emptyList(),
    val favouritesOf: String? = null, // "favorited_by" in api
) : SearchOptions, Parcelable, JsonSerializable {

    // TODO use StringBuilder
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
        debug {
            Log.d(PostsSearchOptions::class.simpleName, "Built query: $query")
        }
        return query
    }

    override suspend fun getPosts(api: IAPI, limit: Int, page: Int): List<Post> {
        return api.getPosts(tags = compileToQuery(), page = page, limit = limit).await().posts
    }

    companion object {
        val DEFAULT =
            PostsSearchOptions(emptyList(), Order.NEWEST_TO_OLDEST, false, emptyList(), null)
    }
}

data class FavouritesSearchOptions(val favouritesOf: String?) : SearchOptions {
    private var id: Int? = null
    override suspend fun getPosts(api: IAPI, limit: Int, page: Int): List<Post> {
        id = id ?: favouritesOf?.let {
            api.getUser(favouritesOf).await().get("id").asInt()
        }
        return api.getFavourites(userId = id, page = page, limit = limit).await().posts
    }
}

class PostsSearchOptionsNavType : NavType<PostsSearchOptions>(false) {
    override fun get(bundle: Bundle, key: String): PostsSearchOptions? {
        return bundle.getParcelable(key)
    }

    override fun parseValue(value: String): PostsSearchOptions {
        return objectMapper.readValue(value)
    }

    override fun put(bundle: Bundle, key: String, value: PostsSearchOptions) {
        bundle.putParcelable(key, value)
    }
}