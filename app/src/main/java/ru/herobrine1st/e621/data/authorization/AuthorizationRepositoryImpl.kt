package ru.herobrine1st.e621.data.authorization

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.herobrine1st.e621.preference.dataStore
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.preference.proto.AuthorizationCredentialsOuterClass.AuthorizationCredentials
import ru.herobrine1st.e621.preference.updatePreferences
import javax.inject.Inject

/**
 * Implementation without multi-account support (Maybe will be added in future)
 */
class AuthorizationRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : AuthorizationRepository {

    private val dataStore = context.dataStore
    private val data = dataStore.getPreferencesFlow {
        if(it.hasAuth()) it.auth
        else null
    }

    override suspend fun getAccount(): AuthorizationCredentials? = data.first()

    override fun getAccountFlow(): Flow<AuthorizationCredentials?> = data

    override suspend fun insertAccount(login: String, password: String) {
        if (getAccountCount() != 0) throw IllegalStateException()
        dataStore.updatePreferences {
            auth = AuthorizationCredentials.newBuilder()
                .setUsername(login)
                .setPassword(password)
                .build()
        }
    }

    override suspend fun logout() {
        dataStore.updatePreferences {
            clearAuth()
        }
    }

    override suspend fun getAccountCount(): Int = data.map { if (it != null) 1 else 0 }.first()
}