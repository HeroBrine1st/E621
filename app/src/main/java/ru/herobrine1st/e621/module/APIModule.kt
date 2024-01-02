/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.module

import android.content.Context
import android.util.Log
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.MessageLengthLimitingLogger
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.preference.proto.AuthorizationCredentialsOuterClass
import ru.herobrine1st.e621.util.AuthorizationNotifier
import ru.herobrine1st.e621.util.USER_AGENT
import ru.herobrine1st.e621.util.credentials
import ru.herobrine1st.e621.util.debug
import java.io.File

class APIModule(
    applicationContext: Context,
    authorizationRepositoryLazy: Lazy<AuthorizationRepository>,
    authorizationNotifierLazy: Lazy<AuthorizationNotifier>
) {

    private val authorizationRepository by authorizationRepositoryLazy
    private val authorizationNotifier by authorizationNotifierLazy

    private val requestDelayMs = 667 // 1.5 req/s, and server limit is 2 req/s
    private val mutex = Mutex()

    @OptIn(ExperimentalSerializationApi::class)
    private val ktorClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    namingStrategy = JsonNamingStrategy.SnakeCase
                    coerceInputValues = true
                })
            }
            install(HttpCache) {
                val cacheDir = File(applicationContext.cacheDir, "ktor")
                publicStorage(FileStorage(cacheDir))
            }

            install(UserAgent) {
                agent = USER_AGENT
            }

            debug {
                install(Logging) {
                    logger = MessageLengthLimitingLogger(
                        delegate = object : Logger {
                            override fun log(message: String) {
                                Log.d("KTor Request", message)
                            }
                        }
                    )
                    level = LogLevel.INFO
                }
            }
        }.apply {
            plugin(HttpSend).apply {
                // Rate limit
                var lastRequestTimeMs = 0L
                intercept { request ->
                    mutex.withLock {
                        if (lastRequestTimeMs + requestDelayMs > System.currentTimeMillis()) {
                            delay(lastRequestTimeMs + requestDelayMs - System.currentTimeMillis())
                        }
                        lastRequestTimeMs = System.currentTimeMillis()
                    }
                    execute(request)
                }

                // Authorization interceptor
                intercept { request ->
                    var auth: AuthorizationCredentialsOuterClass.AuthorizationCredentials? = null
                    if (request.headers[HttpHeaders.Authorization] == null) {
                        auth = authorizationRepository.getAccount()?.also {
                            request.header(HttpHeaders.Authorization, it.credentials)
                        }
                    }
                    val call = execute(request)
                    when (call.response.status) {
                        HttpStatusCode.Unauthorized -> if (auth != null) {
                            authorizationRepository.logout()
                            authorizationNotifier.notifyAuthorizationRevoked()
                        }

                        HttpStatusCode.Forbidden ->
                            Log.w("API Authorization", "Got code 403 - maybe authorization error?")
                    }
                    call
                }
            }
        }
    }

    val apiLazy = lazy {
        Ktorfit.Builder()
            .httpClient(ktorClient)
            .baseUrl(BuildConfig.API_BASE_URL)
            .build()
            .create<API>()
    }

    val api by apiLazy
}