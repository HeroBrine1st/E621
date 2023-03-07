/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.net

import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.preference.proto.AuthorizationCredentialsOuterClass.AuthorizationCredentials
import ru.herobrine1st.e621.util.AuthorizationNotifier
import ru.herobrine1st.e621.util.credentials
import javax.inject.Inject

/**
 * This interceptor is responsible to:
 * * Set Authorization header on every request
 * * Check if authorization is valid and revoke otherwise
 */
class AuthorizationInterceptor @Inject constructor(
    private val authorizationRepository: AuthorizationRepository,
    private val authorizationNotifier: AuthorizationNotifier
) :
    Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var auth: AuthorizationCredentials? = null
        val request = if (chain.request().header("Authorization") == null) {
            // If header isn't set explicitly, set it from database
            auth = runBlocking { authorizationRepository.getAccount() }
            if (auth != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", auth.credentials)
                    .build()
            } else chain.request()
        } else chain.request()
        val response = chain.proceed(request)
        if (response.code == 401) {
            if (auth != null) {
                runBlocking {
                    authorizationRepository.logout()
                }
                authorizationNotifier.notifyAuthorizationRevoked()
                // Maybe retry?
            }
        }
        if(response.code == 403) Log.w(TAG, "Got code 403 - maybe authorization error?")
        return response
    }

    companion object {
        const val TAG = "AuthorizationInterceptor"
    }
}