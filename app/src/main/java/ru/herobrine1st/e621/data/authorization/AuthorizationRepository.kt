package ru.herobrine1st.e621.data.authorization

import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.preference.proto.AuthorizationCredentialsOuterClass.AuthorizationCredentials

// TODO Rename and move it somewhere (it isn't a repository because it may have internal state in future)
interface AuthorizationRepository {
    /**
     * @return This session's credentials
     */
    suspend fun getAccount(): AuthorizationCredentials?

    /**
     * @return Flow of this session's credentials
     */
    fun getAccountFlow(): Flow<AuthorizationCredentials?>

    /**
     * Inserts new credentials
     */
    suspend fun insertAccount(login: String, password: String)

    /**
     * Logs out from this session's credentials
     */
    suspend fun logout()

    /**
     * @return Available accounts count
     */
    suspend fun getAccountCount(): Int
}