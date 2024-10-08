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

package ru.herobrine1st.e621.net

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import ru.herobrine1st.e621.util.StaticValueState

private val mutableSharedFlow = MutableSharedFlow<DownloadProgress>(
    extraBufferCapacity = 5,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

object DownloadProgressInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url
        val response = chain.proceed(chain.request())

        return response.newBuilder()
            .apply {
                response.body?.let { body(ObservingResponseBody(it, url)) }
            }
            .build()
    }

    private class ObservingResponseBody(val delegate: ResponseBody, val httpUrl: HttpUrl) :
        ResponseBody() {
        override fun contentLength(): Long = delegate.contentLength()

        override fun contentType(): MediaType? = delegate.contentType()

        // There's three "read" method call sites, without wrapper I will be copying code
        override fun source(): BufferedSource {
//            mutableSharedFlow.tryEmit(DownloadProgress(httpUrl, 0, contentLength()))
            return ObservingSource(delegate.source(), httpUrl, contentLength()).buffer()
        }
    }

    private class ObservingSource(delegate: Source, val httpUrl: HttpUrl, val contentLength: Long) :
        ForwardingSource(delegate) {
        private var downloaded = 0L
        override fun read(sink: Buffer, byteCount: Long): Long {
            val count = super.read(sink, byteCount)
            downloaded += count
            mutableSharedFlow.tryEmit(DownloadProgress(httpUrl, downloaded, contentLength))
            return count
        }
    }
}

data class DownloadProgress(
    val url: HttpUrl,
    val downloaded: Long,
    val contentLength: Long
) {
    inline val progress get() = downloaded.toFloat() / contentLength
}

/**
 * Collect download progress for [url] as observable [State].
 *
 * Returned state value is null initially, but it is guaranteed that value will never be null once
 * it is not null, if the [url] parameter itself is not null.
 * If the parameter [url] is null, this function will return [State] with null value
 * **regardless of any conditions**.
 *
 * Works only if request is intercepted by [DownloadProgressInterceptor]
 *
 * @param url [HttpUrl] to observe. May be null to disable observing (for convenience)
 * @return Observable state with content length and download progress in bytes.
 */
@Composable
@Stable
fun collectDownloadProgressAsState(url: HttpUrl?) = when (url) {
    null -> remember { StaticValueState(null) }
    else -> remember(url) { mutableSharedFlow.filter { it.url == url } }
        .collectAsState(initial = null)
}