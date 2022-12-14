package ru.herobrine1st.e621.util

import android.os.Build
import android.os.Bundle
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

@OptIn(ExperimentalContracts::class)
inline fun debug(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (BuildConfig.DEBUG) block()
}

val AuthorizationCredentials.credentials get() = Credentials.basic(username, password)

// Google are you happy now????
inline fun <reified T> Bundle.getParcelableCompat(key: String?): T? = when {
    Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key)
}