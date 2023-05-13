/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    @CheckResult
    @GET("/tags/autocomplete.json")
    fun getAutocompleteSuggestions(
        @Query("search[name_matches]") query: String, // 3 or more characters required on the API side
        @Query("expiry") expiry: Int = 7 // idk what it is, use default from site.
    ): Call<List<TagAutocompleteSuggestion>>
}

suspend fun API.getWikiPage(tag: String): WikiPage {
    val firstResponse = getWikiPageId(tag).awaitResponse()
    if (firstResponse.raw().priorResponse == null) throw NotFoundException()
    val id = firstResponse.raw().request.url.pathSegments.last().toIntOrNull()
    if (id == null) {
        Log.e(TAG, "Invalid redirection: cannot extract ID from url")
        Log.e(TAG, firstResponse.raw().headers.joinToString("\n") { (name, value) ->
            "$name: $value"
        })
        throw ApiException("Unknown error", firstResponse.code())
    }
    return getWikiPage(id).await()
}

private const val TAG = "API"