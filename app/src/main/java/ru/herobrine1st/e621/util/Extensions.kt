package ru.herobrine1st.e621.util

import okhttp3.Credentials
import okhttp3.Response
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.entity.Auth
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val Response.isCacheConditional get() = this.networkResponse != null && this.cacheResponse != null

// Like also, but debug
@OptIn(ExperimentalContracts::class)
inline fun <T> T.debug(block: T.() -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (BuildConfig.DEBUG) this.block()
    return this
}

val Auth.credentials get() = Credentials.basic(login, apiKey)