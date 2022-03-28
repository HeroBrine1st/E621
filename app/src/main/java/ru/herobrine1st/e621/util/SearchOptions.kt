package ru.herobrine1st.e621.util

import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavType
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okhttp3.HttpUrl
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.api.Order
import ru.herobrine1st.e621.api.Rating

private val objectMapper = getObjectMapper()

interface SearchOptions {
    val tags: List<String>
    val order: Order?
    val orderAscending: Boolean
    val rating: List<Rating>
    val favouritesOf: String? // "favorited_by" in api

    suspend fun prepareRequestUrl(api: Api): HttpUrl
}

@Parcelize
data class PostsSearchOptions(
    override val tags: List<String> = emptyList(),
    override val order: Order = Order.NEWEST_TO_OLDEST,
    override val orderAscending: Boolean = false,
    override val rating: List<Rating> = emptyList(),
    override val favouritesOf: String? = null,
) : SearchOptions, Parcelable, JsonSerializable {

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

    companion object {
        val DEFAULT = PostsSearchOptions(emptyList(), Order.NEWEST_TO_OLDEST, false, emptyList(), null)
    }

    override fun serializeToJson(): String = objectMapper.writeValueAsString(this)
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