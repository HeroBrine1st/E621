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