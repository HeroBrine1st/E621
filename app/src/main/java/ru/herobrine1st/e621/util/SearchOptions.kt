package ru.herobrine1st.e621.util

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.navigation.NavType
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.parcelize.Parcelize
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.ApiException
import ru.herobrine1st.e621.api.await
import ru.herobrine1st.e621.api.model.Order
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.Rating
import java.io.IOException

interface SearchOptions {
    @Throws(ApiException::class, IOException::class)
    suspend fun getPosts(api: API, limit: Int, page: Int): List<Post>

    fun toBuilder(builder: PostsSearchOptions.Builder.() -> Unit) =
        PostsSearchOptions.builder(this, builder)
}

@Parcelize
data class PostsSearchOptions(
    val tags: List<String> = emptyList(),
    val order: Order = Order.NEWEST_TO_OLDEST,
    val orderAscending: Boolean = false,
    val rating: List<Rating> = emptyList(),
    val favouritesOf: String? = null, // "favorited_by" in api
) : SearchOptions, Parcelable, JsonSerializable {

    private fun compileToQuery(): String {
        val cache = mutableListOf<String>()

        cache.addAll(tags)

        (if (orderAscending) this.order.ascendingApiName else this.order.apiName)?.let {
            cache.add("order:$it")
        }
        if (rating.size < Rating.values().size && rating.isNotEmpty()) {
            if (rating.size == 1) {
                cache.add("rating:${rating[0].apiName}")
            } else {
                cache.addAll(rating.map { "~rating:${it.apiName}" })
            }
        }
        if (favouritesOf != null) {
            cache.add("fav:$favouritesOf")
        }

        return cache.joinToString(" ").debug {
            Log.d(PostsSearchOptions::class.simpleName, "Built query: $this")
        }
    }

    override suspend fun getPosts(api: API, limit: Int, page: Int): List<Post> {
        return api.getPosts(tags = compileToQuery(), page = page, limit = limit).await().posts
    }

    companion object {
        val DEFAULT =
            PostsSearchOptions(emptyList(), Order.NEWEST_TO_OLDEST, false, emptyList(), null)

        fun builder(
            options: SearchOptions? = null,
            builder: Builder.() -> Unit
        ): PostsSearchOptions {
            return when (options) {
                null -> Builder().apply(builder).build()
                else -> Builder.from(options).apply(builder).build()
            }
        }
    }

    class Builder(
        var tags: List<String> = mutableListOf(),
        var order: Order = Order.NEWEST_TO_OLDEST,
        var orderAscending: Boolean = false,
        var rating: List<Rating> = mutableListOf(),
        var favouritesOf: String? = null,
    ) {
        fun build() = PostsSearchOptions(tags, order, orderAscending, rating, favouritesOf)

        companion object {
            fun from(options: SearchOptions) = when (options) {
                is PostsSearchOptions -> with(options) {
                    Builder(tags, order, orderAscending, rating, favouritesOf)
                }
                is FavouritesSearchOptions -> Builder(favouritesOf = options.favouritesOf)
                else -> throw NotImplementedError()
            }
        }
    }
}

data class FavouritesSearchOptions(val favouritesOf: String?) : SearchOptions {
    private var id: Int? = null
    override suspend fun getPosts(api: API, limit: Int, page: Int): List<Post> {
        id = id ?: favouritesOf?.let {
            api.getUser(favouritesOf).await().get("id").asInt()
        }
        return api.getFavourites(userId = id, page = page, limit = limit).await().posts
    }
}

class PostsSearchOptionsNavType : NavType<PostsSearchOptions?>(true) {
    override fun get(bundle: Bundle, key: String): PostsSearchOptions? {
        return bundle.getParcelableCompat(key)
    }

    override fun parseValue(value: String): PostsSearchOptions {
        return objectMapper.readValue(value)
    }

    override fun put(bundle: Bundle, key: String, value: PostsSearchOptions?) {
        bundle.putParcelable(key, value)
    }
}