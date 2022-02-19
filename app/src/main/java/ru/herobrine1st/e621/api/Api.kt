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

fun Response.checkStatus() {
    if (!this.isSuccessful) {
        if (BuildConfig.DEBUG) {
            Log.e(Api.TAG, "Unsuccessful request: $message")
            body?.use {
                Log.d(Api.TAG, "Response body:")
                Log.d(Api.TAG, it.string())
            }
        }
        throw ApiException("Unsuccessful request: $message", code)
    }
}

val LocalAPI = compositionLocalOf<Api> { error("No API found") }

class Api(okHttpClient: OkHttpClient? = null) {
    private val okHttpClient = okHttpClient ?: OkHttpClient.Builder()
        .addInterceptor(RateLimitInterceptor(1.5))
        .build()
    private var credentials: String? = null
    private var login: String? = null
    private val objectMapper = objectMapperFactory()

    private fun updateCredentialsInternal(login: String?, apiKey: String?) {
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
                HttpUrl.Builder()
                    .scheme("https")
                    .host(BuildConfig.API_URL)
                    .addPathSegments("users/$login.json")
                    .build()
            )
            .header("Authorization", Credentials.basic(login, apiKey))
            .build()
        okHttpClient.newCall(req).execute().use {
            if (it.isSuccessful) updateCredentialsInternal(login, apiKey)
            else {
                Log.d(
                    TAG,
                    "Couldn't authorize. Invalid credentials or API blocked. See response below for additional info"
                )
                Log.d(TAG, it.body.use { body -> body?.charStream()?.readText() } ?: "No response")
            }
            return it.isSuccessful
        }
    }

    fun logout() {
        updateCredentialsInternal(null, null)
    }

    private fun requestBuilder(): Request.Builder {
        return Request.Builder()
            .apply { credentials?.let { header("Authorization", it) } }
            .addHeader("Accept", "application/json")
            .header("User-Agent", BuildConfig.USER_AGENT)
    }

    fun getPosts(tags: String, page: Int = 1, limit: Int? = null): List<Post> {
        val req = requestBuilder()
            .url(
                HttpUrl.Builder().apply {
                    scheme("https")
                    host(BuildConfig.API_URL)
                    addPathSegments("posts.json")
                    addEncodedQueryParameter("tags", tags)
                    limit?.let { addQueryParameter("limit", it.toString()) }
                    addQueryParameter("page", page.toString())
                }.build().also { Log.d(TAG, it.toString()) }
            )
            .build()
        okHttpClient.newCall(req).execute().use {
            it.checkStatus()
            return objectMapper.readValue<PostEndpoint>(it.body!!.charStream()).posts
        }
    }

    fun getBlacklistedTags(): List<String> {
        if (credentials == null) {
            Log.w(TAG, "getBlacklistedTags called without credentials available")
            return emptyList()
        }
        val req = requestBuilder()
            .url(
                HttpUrl.Builder()
                    .scheme("https")
                    .host(BuildConfig.API_URL)
                    .addPathSegments("users/$login.json")
                    .build()
            )
            .build()
        okHttpClient.newCall(req).execute().use {
            it.checkStatus()
            return objectMapper.readValue<ObjectNode>(it.body!!.charStream())
                .get("blacklisted_tags").asText()
                .split("\n")

        }
    }

    fun getCommendsForPost(post: Post) = getCommentsForPost(id = post.id)

    @Suppress("MemberVisibilityCanBePrivate")
    fun getCommentsForPost(id: Int): List<Comment> {
        val req = requestBuilder()
            .url(
                HttpUrl.Builder()
                    .scheme("https")
                    .host(BuildConfig.API_URL)
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
            .url(HttpUrl.Builder()
                .scheme("https")
                .host(BuildConfig.API_URL)
                .addPathSegments("favorites.json")
                .addQueryParameter("post_id", postId.toString())
                .build().also { Log.d(TAG, it.toString()) })
            .post("".toRequestBody(null))
            .build()
        okHttpClient.newCall(request).execute().checkStatus()
    }

    fun deleteFavorite(postId: Int) {
        if (credentials == null) {
            Log.w(TAG, "deleteFavorite(int) called without credentials available")
            throw RuntimeException("No credentials available")
        }
        val request = requestBuilder()
            .url(HttpUrl.Builder()
                .scheme("https")
                .host(BuildConfig.API_URL)
                .addPathSegments("favorites/$postId.json")
                .build())
            .delete()
            .build()
        okHttpClient.newCall(request).execute().checkStatus()
    }

    companion object {
        const val TAG = "API"
    }
}