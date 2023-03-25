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

package ru.herobrine1st.e621.net

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import okhttp3.*
import okio.*

private val mutableSharedFlow = MutableSharedFlow<DownloadProgress>(
    extraBufferCapacity = 5,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

private const val TAG = "DownloadProgressInterceptor"

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
    inline val isValid get() = downloaded >= 0
}

@Composable
fun collectDownloadProgressAsState(url: HttpUrl): State<DownloadProgress> =
    remember { mutableSharedFlow.filter { it.url == url } }
        .collectAsState(initial = DownloadProgress(url, -1, 1))