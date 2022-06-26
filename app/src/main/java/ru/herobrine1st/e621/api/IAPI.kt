package ru.herobrine1st.e621.api

import androidx.annotation.IntRange
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import ru.herobrine1st.e621.api.model.PostEndpoint
import ru.herobrine1st.e621.api.model.PostVoteEndpoint
import ru.herobrine1st.e621.api.model.PostsEndpoint

interface IAPI {
    @GET("/users/{name}.json")
    fun getUser(
        @Path("name") name: String,
        @Header("Authorization") credentials: String? = null
    ): Call<ResponseBody>

    @GET("/posts.json")
    fun getPosts(
        @Query("tags") tags: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int? = null
        // @Header("Authorization") credentials: String? = null
    ): Call<PostsEndpoint>

    @GET("/posts/{id}.json")
    fun getPost(
        @Path("id") id: Int
        // @Header("Authorization") credentials: String? = null
    ): Call<PostEndpoint>

    @GET("/favorites.json")
    fun getFavourites(
        @Query("user_id") userId: String?,
        // @Header("Authorization") credentials: String? = null
    ): Call<PostsEndpoint>

    @POST("/favorites.json")
    fun addToFavourites(
        @Query("post_id") postId: Int,
        @Header("Authorization") credentials: String
    ): Call<ResponseBody>

    @DELETE("/favorites/{post_id}.json")
    fun removeFromFavourites(
        @Path("post_id") postId: Int,
        @Header("Authorization") credentials: String
    ): Call<ResponseBody>

    @POST("/posts/{post_id}/votes.json")
    fun vote(
        @Path("post_id") postId: Int,
        @IntRange(from = -1, to = 1) @Query("score") score: Int,
        @Suppress("SpellCheckingInspection") @Query("no_unvote") noRetractVote: Boolean
    ): Call<PostVoteEndpoint>
}