package ru.herobrine1st.e621.net

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import ru.herobrine1st.e621.dao.AuthDao
import ru.herobrine1st.e621.entity.Auth
import ru.herobrine1st.e621.util.credentials
import javax.inject.Inject

/**
 * This interceptor is responsible to:
 * * Set Authorization header on every request
 * * Check if authorization is valid and revoke otherwise
 */
class AuthorizationInterceptor @Inject constructor(val authDao: AuthDao) :
    Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var auth: Auth? = null
        val request = if (chain.request().header("Authorization") == null) {
            // If header isn't set explicitly, set it from database
            auth = runBlocking { authDao.get() }
            if (auth != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", auth.credentials)
                    .build()
            } else chain.request()
        } else chain.request()
        val response = chain.proceed(request)
        // TODO check if codes are right
        if (response.code == 401 || response.code == 403) {
            if(auth != null) {
                runBlocking {
                    authDao.delete(auth)
                }
                // Maybe retry?
            }
        }
        return response
    }
}