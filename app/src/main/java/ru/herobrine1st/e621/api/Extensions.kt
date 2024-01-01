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

import de.jensklingenberg.ktorfit.Response
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.CoroutineContext

suspend fun <T> Response<T>.ensureSuccessful(context: CoroutineContext = Dispatchers.Default) {
    if(status.isSuccess()) return

    if (status == HttpStatusCode.NotFound) {
        throw ApiException("Not found", 404)
    }

    // Try to get JSON with explanation
    val body = try {
        withContext(context) {
            Json.decodeFromString<JsonObject>(errorBody()!!.toString())
        }
    } catch (t: Throwable) {
        // Suppress, it is not the cause neither the actual error
        throw ApiException(
            "Got unsuccessful response $status and could not get response body",
            code
        ).apply {
            addSuppressed(t)
        }
    }
    throw ApiException(
        (body["message"] as? JsonPrimitive?)?.content ?: body.toString(),
        code
    )
}