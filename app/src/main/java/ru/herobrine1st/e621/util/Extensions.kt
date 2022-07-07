package ru.herobrine1st.e621.util

import android.util.Log
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import retrofit2.Call
import retrofit2.awaitResponse
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.ApiException
import ru.herobrine1st.e621.entity.Auth
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

val Auth.credentials get() = Credentials.basic(login, apiKey)


private suspend fun <T> Call<T>.awaitResponseInternal(): retrofit2.Response<T> {
    val response = this.awaitResponse()
    if (response.code() !in 200..399) { // Include redirects
        val body = withContext(Dispatchers.IO) {
            objectMapper.readValue<ObjectNode>(response.errorBody()!!.charStream())
        }
        val message = body.get("message")?.asText() ?: body.toPrettyString()
        Log.e("API", "Got unsuccessful response: ${response.code()} ${response.message()}")
        Log.e("API", message)
        throw ApiException(message, response.code())
    }
    return response
}

suspend fun <T> Call<T>.awaitResponse(): retrofit2.Response<T> = this.awaitResponseInternal()

suspend fun <T> Call<T>.await(): T {
    val response = this.awaitResponseInternal()
    val body = response.body()
    // 204 is "No Content" meaning body length is 0 bytes and is not "Null Response" :///
    if (response.code() == 204 && body == null) {
        Log.e(
            "API",
            "Response code is 204, therefore body is null, use awaitResponse() instead"
        )
    }
    return response.body()!!
}