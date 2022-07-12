package ru.herobrine1st.e621.api

import android.util.Log
import androidx.annotation.CheckResult
import androidx.annotation.IntRange
import com.fasterxml.jackson.databind.node.ObjectNode
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import ru.herobrine1st.e621.api.model.*

interface API {
    @CheckResult
    @GET("/users/{name}.json")
    fun getUser(
        @Path("name") name: String,
        @Header("Authorization") credentials: String? = null
    ): Call<ObjectNode>

    @CheckResult
    @GET("/posts.json")
    fun getPosts(
        @Query("tags") tags: String? = null,
        @Query("page") page: Int? = null, // 1 by default
        @Query("limit") limit: Int? = null, // 250 by default
        // @Header("Authorization") credentials: String? = null
    ): Call<PostsEndpoint>

    @CheckResult
    @GET("/posts/{id}.json")
    fun getPost(
        @Path("id") id: Int
        // @Header("Authorization") credentials: String? = null
    ): Call<PostEndpoint>

    @CheckResult
    @GET("/favorites.json")
    fun getFavourites(
        @Query("user_id") userId: Int? = null,
        @Query("page") page: Int? = null, // 1 by default
        @Query("limit") limit: Int? = null, // 250 by default
        // @Header("Authorization") credentials: String? = null
    ): Call<PostsEndpoint>

    @CheckResult
    @POST("/favorites.json")
    fun addToFavourites(
        @Query("post_id") postId: Int,
//        @Header("Authorization") credentials: String
    ): Call<ResponseBody>

    @CheckResult
    @DELETE("/favorites/{post_id}.json")
    fun removeFromFavourites(
        @Path("post_id") postId: Int,
//        @Header("Authorization") credentials: String
    ): Call<Void>

    @CheckResult
    @POST("/posts/{post_id}/votes.json")
    fun vote(
        @Path("post_id") postId: Int,
        @IntRange(from = -1, to = 1) @Query("score") score: Int,
        @Suppress("SpellCheckingInspection") @Query("no_unvote") noRetractVote: Boolean,
//        @Header("Authorization") credentials: String
    ): Call<PostVoteEndpoint>

    /**
     * Redirects to another page `/wiki_pages/(\d+)` if found, else no redirect and code 200
     */
    @CheckResult
    @GET("/wiki_pages/show_or_new")
    fun getWikiPageId(@Query("title") title: String): Call<ResponseBody>

    @CheckResult
    @GET("/wiki_pages/{id}.json")
    fun getWikiPage(@Path("id") id: Int): Call<WikiPage>

    /*
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
     */

    @CheckResult
    @GET("/posts/{post_id}/comments.json")
    fun getCommentsForPostHTML(
        @Path("post_id") id: Int
    ): Call<PostCommentsEndpoint>

    @CheckResult
    @GET("/comments.json?group_by=comment")
    fun getCommentsForPostBBCode(
        @Query("search[post_id]") id: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int // Default unknown. Maybe 75, but I doubt
    ): Call<List<CommentBB>>
}

suspend fun API.getWikiPage(tag: String): WikiPage {
    val firstResponse = getWikiPageId(tag).awaitResponse()
    if (!firstResponse.raw().isRedirect) {
        throw NotFoundException()
    }
    val id = firstResponse.raw().header("Location")?.let {
        it.substring(it.lastIndexOf("/") + 1).toIntOrNull()
    }
    if (id == null) {
        Log.e(TAG, "Invalid redirection: Location header is not found or is not parsed")
        Log.e(TAG, firstResponse.raw().headers.joinToString("\n") {
            it.first + ": " + it.second
        })
        throw ApiException("Unknown error", firstResponse.code())
    }
    return getWikiPage(id).await()
}

private const val TAG = "API"