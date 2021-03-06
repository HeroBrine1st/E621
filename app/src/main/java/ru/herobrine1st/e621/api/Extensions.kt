package ru.herobrine1st.e621.api

import android.util.Log
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response
import retrofit2.awaitResponse
import ru.herobrine1st.e621.util.objectMapper

private suspend fun <T> Call<T>.awaitResponseInternal(): Response<T> {
    val response = this.awaitResponse()
    if (response.code() !in 200..399) { // Include redirects
        val message = kotlin.run {
            if(response.code() == 404) return@run "Not found"
            val body = withContext(Dispatchers.IO) {
                objectMapper.readValue<ObjectNode>(response.errorBody()!!.charStream())
            }
            body.get("message")?.asText() ?: body.toPrettyString()
        }

        Log.e("API", "Got unsuccessful response: ${response.code()} ${response.message()}")
        Log.e("API", message)
        throw ApiException(message, response.code())
    }
    return response
}

suspend fun <T> Call<T>.awaitResponse(): Response<T> = this.awaitResponseInternal()
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