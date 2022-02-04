package ru.herobrine1st.e621.net

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class UserAgentInterceptor(private val userAgent: String) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val requestWithUserAgent: Request = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}