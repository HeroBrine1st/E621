/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2025 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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
/*
 * This file is a derived work.
 * Changes to coil3 ktor network fetcher: add progress callback, inline functions.
 *
 * Copyright 2024 Coil Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.herobrine1st.e621.util

import coil3.Extras
import coil3.network.NetworkClient
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.http.takeFrom
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.jvm.nio.copyTo
import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import java.io.RandomAccessFile
import kotlin.jvm.JvmInline

val progressCallbackExtra =
    Extras.Key<(bytesSentTotal: Long, contentLength: Long) -> Unit>(default = { _, _ -> })

@JvmInline
value class CoilKtorNetworkFetcher(
    private val httpClient: HttpClient,
) : NetworkClient {
    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ) = httpClient.prepareRequest {
        url.takeFrom(request.url)
        method = HttpMethod.parse(request.method)
        for ((key, values) in request.headers.asMap()) {
            headers.appendAll(key, values)
        }
        request.body?.let { body ->
            Buffer().also { buffer ->
                body.writeTo(buffer)
            }.readByteArray()
        }?.let {
            setBody(it)
        }
        request.extras[progressCallbackExtra]
            ?.let {
                onDownload(it)
            }
    }.execute { response ->
        block(
            NetworkResponse(
                code = response.status.value,
                requestMillis = response.requestTime.timestamp,
                responseMillis = response.responseTime.timestamp,
                headers = NetworkHeaders.Builder().apply<NetworkHeaders.Builder> {
                    response.headers.entries()
                        .forEach<Map.Entry<String, List<String>>> { (key, values) ->
                            this[key] = values
                        }
                }.build(),
                body = KtorNetworkResponseBody(response.bodyAsChannel()),
                delegate = response,
            )
        )
    }
}

@JvmInline
private value class KtorNetworkResponseBody(
    private val channel: ByteReadChannel,
) : NetworkResponseBody {

    override suspend fun writeTo(sink: BufferedSink) {
        channel.writeTo(sink)
    }

    override suspend fun writeTo(fileSystem: FileSystem, path: Path) {
        channel.writeTo(fileSystem, path)
    }

    override fun close() {
        channel.cancel()
    }
}

private suspend fun ByteReadChannel.writeTo(sink: BufferedSink) {
    copyTo(sink)
}

private suspend fun ByteReadChannel.writeTo(fileSystem: FileSystem, path: Path) {
    if (fileSystem === FileSystem.SYSTEM) {
        // Fast path: normal jvm File, write to FileChannel directly.
        RandomAccessFile(path.toFile(), "rw").use {
            copyTo(it.channel)
        }
    } else {
        // Slow path: cannot guarantee a "real" file.
        fileSystem.write(path) {
            copyTo(this)
        }
    }
}