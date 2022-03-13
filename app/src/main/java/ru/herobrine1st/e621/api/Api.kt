package ru.herobrine1st.e621.api

import android.util.Log
import androidx.compose.runtime.compositionLocalOf
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.model.*
import ru.herobrine1st.e621.net.RateLimitInterceptor

fun objectMapperFactory(): ObjectMapper = jsonMapper {
    addModule(kotlinModule())
    addModule(JavaTimeModule())
}.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)

fun Response.checkStatus(close: Boolean = false, noThrow: Boolean = false) {
    if (!this.isSuccessful) {
        if (BuildConfig.DEBUG) {
            Log.e(Api.TAG, "Unsuccessful request: $message")
            body?.use {
                Log.d(Api.TAG, "Response body:")
                Log.d(Api.TAG, it.string())
            }
        }
        if (noThrow) return
        body?.close()
        throw ApiException("Unsuccessful request: $message", code)
    }
    if (close) body?.close()
}

val LocalAPI = compositionLocalOf<Api> { error("No API found") }

class Api(okHttpClient: OkHttpClient? = null) {
    private val okHttpClient = okHttpClient ?: OkHttpClient.Builder()
        .addInterceptor(RateLimitInterceptor(1.5))
        .build()
    private var credentials: String? = null
    var login: String? = null
        private set
    private val objectMapper = objectMapperFactory()

    private fun updateCredentialsInternal(login: String?, apiKey: String? = null) {
        assert(login != null || apiKey == null)
        this.login = login
        credentials = if (login != null && apiKey != null) Credentials.basic(login, apiKey)
        else null
    }

    /**
     * Tries to login with provided credentials and saves them if succeeded
     * @param login Login
     * @param apiKey Api key
     * @return True if credentials valid
     */
    fun checkCredentials(login: String, apiKey: String): Boolean {
        val req = requestBuilder()
            .url(
                API_BASE_URL.newBuilder()
                    .addPathSegments("users/$login.json")
                    .build()
            )
            .header("Authorization", Credentials.basic(login, apiKey))
            .build()
        okHttpClient.newCall(req).execute().use {
            it.checkStatus(noThrow = true)
            if (it.isSuccessful) updateCredentialsInternal(login, apiKey)
            return it.isSuccessful
        }
    }

    fun logout() {
        updateCredentialsInternal(null)
    }

    private fun requestBuilder(): Request.Builder {
        return Request.Builder()
            .apply { credentials?.let { header("Authorization", it) } }
            .addHeader("Accept", "application/json")
            .header("User-Agent", BuildConfig.USER_AGENT)
    }

    fun getPosts(tags: String, page: Int = 1, limit: Int? = null): List<Post> {
        return getPosts(preparePostsRequestUrl(tags), page, limit)
    }

    fun getPosts(preparedUrl: HttpUrl, page: Int = 1, limit: Int? = null): List<Post> {
        val req = requestBuilder()
            .url(
                preparedUrl.newBuilder().apply {
                    limit?.let { addQueryParameter("limit", it.toString()) }
                    addQueryParameter("page", page.toString())
                }.build().also { Log.d(TAG, it.toString()) }
            )
            .build()
        okHttpClient.newCall(req).execute().use {
            it.checkStatus()
            return objectMapper.readValue<PostsEndpoint>(it.body!!.charStream()).posts
        }
    }

    fun getPost(id: Int): Post {
        val req = requestBuilder()
            .url(
                API_BASE_URL.newBuilder()
                    .addPathSegments("posts/$id.json")
                    .build().also { Log.d(TAG, it.toString()) }
            )
            .build()
        okHttpClient.newCall(req).execute().use {
            it.checkStatus()
            return objectMapper.readValue<PostEndpoint>(it.body!!.charStream()).post
        }
    }

    private fun getUserByName(user: String): ObjectNode {
        val req = requestBuilder()
            .url(
                API_BASE_URL.newBuilder()
                    .addPathSegments("users/$user.json")
                    .build()
            )
            .build()
        okHttpClient.newCall(req).execute().use {
            it.checkStatus()
            return objectMapper.readValue(it.body!!.charStream())
        }
    }

    fun getBlacklistedTags(): List<String> {
        if (credentials == null) {
            Log.w(TAG, "getBlacklistedTags called without credentials available")
            return emptyList()
        }
        return getUserByName(login!!)
            .get("blacklisted_tags").asText()
            .split("\n")
    }

    fun getIdOfUser(user: String): Int {
        return getUserByName(user)
            .get("id").asInt()
    }

    fun getCommendsForPost(post: Post) = getCommentsForPost(id = post.id)

    @Suppress("MemberVisibilityCanBePrivate")
    fun getCommentsForPost(id: Int): List<Comment> {
        // Получить комментарии:
        // GET /comments.json?group_by=comment&search[post_id]=$id&page=$page
        // Не даст ни постов, ни маппинга юзер->аватарка, но даст адекватные комментарии
        // Посты и маппинги можно получить кодом ниже
        val req = requestBuilder()
            .url(
                API_BASE_URL.newBuilder()
                    .addPathSegments("posts/$id/comments.json")
                    .build()
            )
            .build()
        val response = okHttpClient.newCall(req).execute().use {
            it.checkStatus()
            objectMapper.readValue<PostCommentsEndpoint>(it.body!!.charStream())
        }
        return parseComments(response)
    }

    fun favorite(postId: Int) {
        if (credentials == null) {
            Log.w(TAG, "favorite(int) called without credentials available")
            throw RuntimeException("No credentials available")
        }
        val request = requestBuilder()
            .url(API_BASE_URL.newBuilder()
                .addPathSegments("favorites.json")
                .addQueryParameter("post_id", postId.toString())
                .build().also { Log.d(TAG, it.toString()) })
            .post("".toRequestBody(null))
            .build()
        okHttpClient.newCall(request).execute().checkStatus(true)
    }

    fun deleteFavorite(postId: Int) {
        if (credentials == null) {
            Log.w(TAG, "deleteFavorite(int) called without credentials available")
            throw RuntimeException("No credentials available")
        }
        val request = requestBuilder()
            .url(
                API_BASE_URL.newBuilder()
                    .addPathSegments("favorites/$postId.json")
                    .build()
            )
            .delete()
            .build()
        okHttpClient.newCall(request).execute().checkStatus(true)
    }

    fun vote(postId: Int, score: Int, noUnvote: Boolean = false): PostVoteEndpoint {
        if (credentials == null) {
            throw RuntimeException("No credentials available")
        }
        val request = requestBuilder()
            .url(API_BASE_URL.newBuilder()
                .addPathSegments("posts/$postId/votes.json")
                .addQueryParameter("score", score.toString())
                .addQueryParameter("no_unvote", noUnvote.toString())
                .build().also { Log.d(TAG, it.toString()) })
            .post("".toRequestBody(null))
            .build()
        okHttpClient.newCall(request).execute().use {
            it.checkStatus()
            return objectMapper.readValue(it.body!!.charStream())
        }
    }


    companion object {
        const val TAG = "API"
        private val API_BASE_URL = HttpUrl.Builder()
            .scheme("https")
            .host(BuildConfig.API_HOST)
            .build()

        fun preparePostsRequestUrl(tags: String): HttpUrl {
            return API_BASE_URL.newBuilder()
                .addPathSegments("posts.json")
                .addQueryParameter("tags", tags)
                .build()
        }

        fun prepareFavouritesRequestUrl(userId: Int? = null): HttpUrl {
            return API_BASE_URL.newBuilder()
                .addPathSegments("favorites.json")
                .apply { userId?.let { addQueryParameter("user_id", it.toString()) } }
                .build()
        }

    }
}