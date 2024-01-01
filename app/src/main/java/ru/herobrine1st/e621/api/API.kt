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

import androidx.annotation.CheckResult
import androidx.annotation.IntRange
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import ru.herobrine1st.e621.api.model.CommentBB
import ru.herobrine1st.e621.api.model.Pool
import ru.herobrine1st.e621.api.model.PostCommentsEndpoint
import ru.herobrine1st.e621.api.model.PostEndpoint
import ru.herobrine1st.e621.api.model.PostVoteEndpoint
import ru.herobrine1st.e621.api.model.PostsEndpoint
import ru.herobrine1st.e621.api.model.TagAutocompleteSuggestion
import ru.herobrine1st.e621.api.model.WikiPage

interface API {
    // TODO proper model
    @CheckResult
    @GET("/users/{name}.json")
    suspend fun getUser(
        @Path("name") name: String
    ): JsonObject

    @CheckResult
    @GET("/users/{name}.json")
    suspend fun authCheck(
        @Path("name") name: String,
        @Header("Authorization") credentials: String? = null
    ): Response<JsonObject>

    @CheckResult
    @GET("/posts.json")
    suspend fun getPosts(
        @Query("tags") tags: String? = null,
        @Query("page") page: Int? = null, // 1 by default
        @Query("limit") limit: Int? = null, // 250 by default
        // @Header("Authorization") credentials: String? = null
    ): PostsEndpoint

    @CheckResult
    @GET("/posts/{id}.json")
    suspend fun getPost(
        @Path("id") id: Int
        // @Header("Authorization") credentials: String? = null
    ): PostEndpoint

    @CheckResult
    @GET("/favorites.json")
    suspend fun getFavourites(
        @Query("user_id") userId: Int? = null,
        @Query("page") page: Int? = null, // 1 by default
        @Query("limit") limit: Int? = null, // 250 by default
        // @Header("Authorization") credentials: String? = null
    ): PostsEndpoint

    @CheckResult
    @POST("/favorites.json")
    suspend fun addToFavourites(
        @Query("post_id") postId: Int,
//        @Header("Authorization") credentials: String
    ): Response<JsonElement>

    @CheckResult
    @DELETE("/favorites/{post_id}.json")
    suspend fun removeFromFavourites(
        @Path("post_id") postId: Int,
//        @Header("Authorization") credentials: String
    ): Response<Void>

    @CheckResult
    @POST("/posts/{post_id}/votes.json")
    suspend fun vote(
        @Path("post_id") postId: Int,
        @IntRange(from = -1, to = 1) @Query("score") score: Int,
        @Suppress("SpellCheckingInspection") @Query("no_unvote") noRetractVote: Boolean,
//        @Header("Authorization") credentials: String
    ): PostVoteEndpoint

    @CheckResult
    @GET("/wiki_pages/{name}.json")
    // TODO change to Tag when it is a value class
    suspend fun getWikiPage(@Path("name") name: String): WikiPage

    @CheckResult
    @GET("/posts/{post_id}/comments.json")
    suspend fun getCommentsForPostHTML(
        @Path("post_id") id: Int
    ): PostCommentsEndpoint

    @CheckResult
    @GET("/comments.json?group_by=comment")
    suspend fun getCommentsForPostBBCode(
        @Query("search[post_id]") id: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int // Default unknown. Maybe 75, but I doubt
    ): List<CommentBB>

    @CheckResult
    @GET("/tags/autocomplete.json")
    suspend fun getAutocompleteSuggestions(
        @Query("search[name_matches]") query: String, // 3 or more characters required on the API side
        @Query("expiry") expiry: Int = 7 // idk what it is, use default from site.
    ): List<TagAutocompleteSuggestion>

    @CheckResult
    @GET("/pools/{poolId}.json")
    suspend fun getPool(@Path("poolId") poolId: Int): Pool
}