/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.basicAuth
import io.ktor.client.request.setBody
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import ru.herobrine1st.e621.api.endpoint.APIEndpoint
import ru.herobrine1st.e621.api.endpoint.favourites.AddToFavouritesEndpoint
import ru.herobrine1st.e621.api.endpoint.favourites.GetFavouritesEndpoint
import ru.herobrine1st.e621.api.endpoint.favourites.RemoveFromFavouritesEndpoint
import ru.herobrine1st.e621.api.endpoint.pools.GetPoolEndpoint
import ru.herobrine1st.e621.api.endpoint.posts.GetPostCommentsDTextEndpoint
import ru.herobrine1st.e621.api.endpoint.posts.GetPostCommentsHTMLEndpoint
import ru.herobrine1st.e621.api.endpoint.posts.GetPostEndpoint
import ru.herobrine1st.e621.api.endpoint.posts.GetPostsEndpoint
import ru.herobrine1st.e621.api.endpoint.posts.VoteEndpoint
import ru.herobrine1st.e621.api.endpoint.tags.GetAutocompleteSuggestionsEndpoint
import ru.herobrine1st.e621.api.endpoint.users.GetUserEndpoint
import ru.herobrine1st.e621.api.endpoint.wiki.GetWikiPageEndpoint
import ru.herobrine1st.e621.api.model.Pool
import ru.herobrine1st.e621.api.model.PostId
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.preference.AuthorizationCredentials
import io.ktor.client.plugins.resources.request as ktorRequest

class APIImpl(val client: HttpClient) : API {
    override suspend fun getUser(name: String) =
        client.request(GetUserEndpoint(name))

    override suspend fun getPosts(
        tags: String?,
        page: Int?,
        limit: Int?,
    ) = client.request(
        GetPostsEndpoint(tags, page, limit)
    )

    override suspend fun getPost(id: PostId) = client.request(GetPostEndpoint(id))

    override suspend fun getFavourites(
        userId: Int?,
        page: Int?,
        limit: Int?,
    ) = client.request(GetFavouritesEndpoint(userId, page, limit))

    override suspend fun addToFavourites(postId: PostId): Result<AddToFavouritesEndpoint.Response> =
        client.request(AddToFavouritesEndpoint(postId))

    override suspend fun removeFromFavourites(postId: PostId) =
        client.request(RemoveFromFavouritesEndpoint(postId))

    override suspend fun vote(postId: PostId, score: Int, noRetractVote: Boolean) =
        client.request(VoteEndpoint(postId, score, noRetractVote))

    override suspend fun getWikiPage(tag: Tag) = client.request(GetWikiPageEndpoint(tag))

    override suspend fun getCommentsForPostHTML(id: PostId) =
        client.request(GetPostCommentsHTMLEndpoint(id))

    override suspend fun getCommentsForPostBBCode(id: PostId, page: Int, limit: Int) =
        client.request(GetPostCommentsDTextEndpoint(id, page, limit))

    override suspend fun getAutocompleteSuggestions(
        query: String,
        expiry: Int,
    ) = client.request(GetAutocompleteSuggestionsEndpoint(query, expiry))

    override suspend fun getPool(poolId: Int): Result<Pool> =
        client.request(GetPoolEndpoint(poolId))

    override suspend fun authenticate(username: String, apiKey: String) =
        client.request(GetUserEndpoint(username)) {
            basicAuth(username, apiKey)
        }.map {
            AuthorizationCredentials(
                username = it["name"]!!.jsonPrimitive.content,
                apiKey = apiKey,
                id = it["id"]!!.jsonPrimitive.int
            )
        }
}

private suspend inline fun <reified Endpoint : APIEndpoint<Unit, Response>, reified Response> HttpClient.request(
    endpoint: Endpoint,
    builder: HttpRequestBuilder.() -> Unit = {},
) = request(endpoint, Unit, builder)

private suspend inline fun <reified Endpoint : APIEndpoint<Request, Response>, reified Request, reified Response> HttpClient.request(
    endpoint: Endpoint,
    body: Request,
    builder: HttpRequestBuilder.() -> Unit = {},
): Result<Response> {
    return try {
        Result.success(requestInternal(endpoint, body, builder))
    } catch (e: ResponseException) {
        Result.failure(e.toApiException())
    } catch (t: Throwable) {
        Result.failure(t)
    }
}


@OptIn(ExperimentalSerializationApi::class)
private suspend inline fun <reified Endpoint : APIEndpoint<Request, Response>, reified Request, reified Response> HttpClient.requestInternal(
    endpoint: Endpoint,
    body: Request,
    builder: HttpRequestBuilder.() -> Unit = {},
): Response {
    val response = ktorRequest(endpoint) {
        val annotations = serializer<Endpoint>().descriptor.annotations
        val method = annotations.filterIsInstance<HttpMethod>()
            .firstOrNull() ?: error("APIEndpoint requires @HttpMethod annotation")
        this.method = method.toKtorMethod()
        if (body != Unit) setBody(body)
        builder()
    }

    return response.body()
}

private fun HttpMethod.toKtorMethod(): io.ktor.http.HttpMethod {
    return io.ktor.http.HttpMethod.parse(method.name)
}

private suspend fun ResponseException.toApiException(): ApiException {
    val status = response.status
    val responseBody = try {
        response.body<JsonObject>()
    } catch (t: Throwable) {
        // Suppress, it is not the cause neither the actual error
        return ApiException(
            "Got unsuccessful response $status and could not get response body",
            status,
            cause = this
        ).apply {
            addSuppressed(t)
        }
    }
    return ApiException(
        (responseBody["message"] as? JsonPrimitive?)?.content
            ?: responseBody.toString(),
        status,
        cause = this
    )
}