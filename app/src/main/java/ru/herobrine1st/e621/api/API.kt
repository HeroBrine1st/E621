package ru.herobrine1st.e621.api

import androidx.annotation.CheckResult
import androidx.annotation.IntRange
import com.fasterxml.jackson.databind.node.ObjectNode
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import ru.herobrine1st.e621.api.model.PostEndpoint
import ru.herobrine1st.e621.api.model.PostVoteEndpoint
import ru.herobrine1st.e621.api.model.PostsEndpoint
import ru.herobrine1st.e621.api.model.WikiPage

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
}