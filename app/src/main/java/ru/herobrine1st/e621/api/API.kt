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

import androidx.annotation.IntRange
import kotlinx.serialization.json.JsonObject
import ru.herobrine1st.e621.api.endpoint.favourites.AddToFavouritesEndpoint
import ru.herobrine1st.e621.api.endpoint.favourites.GetFavouritesEndpoint
import ru.herobrine1st.e621.api.endpoint.posts.GetPostCommentsDTextEndpoint
import ru.herobrine1st.e621.api.endpoint.posts.GetPostCommentsHTMLEndpoint
import ru.herobrine1st.e621.api.endpoint.posts.GetPostEndpoint
import ru.herobrine1st.e621.api.endpoint.posts.GetPostsEndpoint
import ru.herobrine1st.e621.api.endpoint.posts.VoteEndpoint
import ru.herobrine1st.e621.api.model.Pool
import ru.herobrine1st.e621.api.model.PostId
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.api.model.TagAutocompleteSuggestion
import ru.herobrine1st.e621.api.model.WikiPage

interface AutocompleteSuggestionsAPI {
    suspend fun getAutocompleteSuggestions(
        query: String, // 3 or more characters required on the API side
        expiry: Int = 7, // idk what it is, use default from site.
    ): Result<List<TagAutocompleteSuggestion>>
}

interface API : AutocompleteSuggestionsAPI {
    // TODO proper model
    suspend fun getUser(
        name: String,
        authorization: String? = null,
    ): Result<JsonObject>

    suspend fun getPosts(
        tags: String? = null,
        page: Int? = null, // 1 by default
        limit: Int? = null, // 250 by default
    ): Result<GetPostsEndpoint.Response>


    suspend fun getPost(
        id: PostId,
    ): Result<GetPostEndpoint.Response>


    suspend fun getFavourites(
        userId: Int? = null,
        page: Int? = null, // 1 by default
        limit: Int? = null, // 250 by default
    ): Result<GetFavouritesEndpoint.Response>

    suspend fun addToFavourites(
        postId: PostId,
    ): Result<AddToFavouritesEndpoint.Response>


    suspend fun removeFromFavourites(
        postId: PostId,
    ): Result<Unit>


    suspend fun vote(
        postId: PostId,
        @IntRange(from = -1, to = 1) score: Int,
        noRetractVote: Boolean,
    ): Result<VoteEndpoint.Response>

    suspend fun getWikiPage(tag: Tag): Result<WikiPage>

    suspend fun getCommentsForPostHTML(
        id: PostId,
    ): Result<GetPostCommentsHTMLEndpoint.Response>


    suspend fun getCommentsForPostBBCode(
        id: PostId,
        page: Int,
        limit: Int, // Default unknown. Maybe 75, but I doubt
    ): Result<GetPostCommentsDTextEndpoint.Response>


    suspend fun getPool(poolId: Int): Result<Pool>

    //    //region Up/down votes
//
//    suspend fun vote(post: Post, vote: Int) {
//        assert(vote in -1..1)
//        val currentVote = database.voteDao().getVote(post.id) ?: 0
//        if (vote == 0) {
//            val score = api.vote(post.id, currentVote, false)
//            if (score.ourScore != 0) { // API does not send user's vote with post
//                assert(api.vote(post.id, score.ourScore, false).ourScore == 0)
//            }
//        } else {
//            assert(api.vote(post.id, vote, true).ourScore == vote)
//        }
//        database.voteDao().insertOrUpdate(post.id, vote)
//    }
//
//    suspend fun getPostVote(post: Post): Int {
//        return database.voteDao().getVote(post.id) ?: 0
//    }
}