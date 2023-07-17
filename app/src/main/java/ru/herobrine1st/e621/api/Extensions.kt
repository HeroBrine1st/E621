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
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response
import ru.herobrine1st.e621.util.objectMapper
import kotlin.coroutines.CoroutineContext

private suspend fun <T> Call<T>.awaitResponseInternal(context: CoroutineContext = Dispatchers.IO): Response<T> {
    // Not really await, but under the hood it uses MainThreadExecutor
    // No point in overriding it, we have Dispatchers.IO
    val response = withContext(context) {
        this@awaitResponseInternal.execute()
    }

    // Include redirects
    if (response.code() in 200..399) return response

    // Attempt to get more readable error
    // At cost of less readable code, of course

    // Maybe it is proven to lie, maybe it is an optimization, idk
    if (response.code() == 404) {
        throw ApiException("Not found", 404)
    }

    // Try to get JSON with explanation
    val body = try {
        withContext(context) {
            objectMapper.readValue<ObjectNode>(response.errorBody()!!.charStream())
        }
    } catch (t: Throwable) {
        // Suppress, it is not the cause neither the actual error
        throw ApiException(
            "Got unsuccessful response with code ${response.code()} and could not get response body",
            response.code()
        ).apply {
            addSuppressed(t)
        }
    }
    throw ApiException(
        body.get("message")?.asText() ?: body.toPrettyString(),
        response.code()
    )

}

suspend fun <T> Call<T>.awaitResponse(context: CoroutineContext = Dispatchers.IO): Response<T> =
    this.awaitResponseInternal(context)

suspend fun <T> Call<T>.await(context: CoroutineContext = Dispatchers.IO): T {
    val response = this.awaitResponseInternal(context)

    val body = response.body()
    // 204 is "No Content" meaning body length is 0 bytes and is not "Null Response" :///
    if (response.code() == 204 && body == null) {
        Log.wtf(
            "API",
            "Response code is 204, therefore body is null, use awaitResponse() instead"
        )
    }
    return body!!
}