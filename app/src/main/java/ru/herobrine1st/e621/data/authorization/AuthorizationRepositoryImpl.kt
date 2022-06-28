package ru.herobrine1st.e621.data.authorization

import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.dao.AuthDao
import ru.herobrine1st.e621.entity.Auth
import javax.inject.Inject

/**
 * Implementation without multi-account support (Maybe will be added in future)
 */
class AuthorizationRepositoryImpl @Inject constructor(val authDao: AuthDao) :
    AuthorizationRepository {
    override suspend fun getAccount(): Auth? = authDao.get()

    override fun getAccountFlow(): Flow<Auth?> = authDao.getFlow()

    override suspend fun insertAccount(login: String, password: String) {
        if (getAccountCount() != 0) throw IllegalStateException()
        authDao.insert(Auth(login, password))
    }

    override suspend fun logout() {
        authDao.deleteAll()
    }

    override suspend fun getAccountCount(): Int = authDao.count()
}