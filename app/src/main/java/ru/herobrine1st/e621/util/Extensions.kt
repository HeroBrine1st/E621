package ru.herobrine1st.e621.util

import okhttp3.Credentials
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.preference.proto.AuthorizationCredentialsOuterClass.AuthorizationCredentials
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// Like also, but debug
@OptIn(ExperimentalContracts::class)
inline fun <T> T.debug(block: T.() -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (BuildConfig.DEBUG) this.block()
    return this
}

val AuthorizationCredentials.credentials get() = Credentials.basic(username, password)