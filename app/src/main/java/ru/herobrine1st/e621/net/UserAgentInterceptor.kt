package ru.herobrine1st.e621.net

import okhttp3.Interceptor
import okhttp3.Response
import ru.herobrine1st.e621.BuildConfig

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request()
            .newBuilder()
            .header("User-Agent", BuildConfig.USER_AGENT)
            .build()
    )
}