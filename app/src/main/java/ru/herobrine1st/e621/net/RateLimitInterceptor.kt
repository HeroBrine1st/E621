/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Blocking interceptor that allows only one request at the same time and specified requests per second
 *
 * @param requestsPerSecond Requests per second
 */
class RateLimitInterceptor(requestsPerSecond: Double): Interceptor {
    private var lastRequestTimeMs = 0L
    private val requestWindowMs = (1000 / requestsPerSecond).toLong()
    override fun intercept(chain: Interceptor.Chain): Response {
        synchronized(this) {
            if(lastRequestTimeMs + requestWindowMs > System.currentTimeMillis()) {
                Thread.sleep(lastRequestTimeMs + requestWindowMs - System.currentTimeMillis())
            }
            lastRequestTimeMs = System.currentTimeMillis()
        }
        return chain.proceed(chain.request())
    }
}