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