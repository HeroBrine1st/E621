package ru.herobrine1st.e621.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.Response
import retrofit2.Call
import retrofit2.awaitResponse
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.ApiException
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

suspend fun <T> Call<T>.await(): T {
    val response = this.awaitResponse()
    if (!response.isSuccessful) {
        val bodyResult = withContext(Dispatchers.IO) {
            kotlin.runCatching {
                response.errorBody()!!.string()
            }
        }
        Log.e("API", "Got unsuccessful response: ${response.code()} ${response.message()}")
        if (bodyResult.isSuccess) {
            Log.e("API", bodyResult.getOrThrow())
        } else {
            Log.e(
                "API",
                "An exception occurred while reading error response",
                // why only nullable getter ???
                bodyResult.exceptionOrNull()!!
            )
        }
        throw ApiException(
            // TODO replace with deserialization
            bodyResult.getOrDefault("Unknown API exception occurred"),
            response.code(),
            bodyResult.exceptionOrNull()
        )
    }
    return response.body()!!
}