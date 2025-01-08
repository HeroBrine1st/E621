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

package ru.herobrine1st.e621.database.repository.authorization

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.herobrine1st.e621.module.DataStoreModule
import ru.herobrine1st.e621.preference.AuthorizationCredentials

/**
 * Implementation without multi-account support (Maybe will be added in future)
 */
class AuthorizationRepositoryImpl(private val dataStoreModule: DataStoreModule) :
    AuthorizationRepository {

    private val data = dataStoreModule.data.map { it.auth }

    override suspend fun getAccount(): AuthorizationCredentials? = data.first()

    override fun getAccountFlow(): Flow<AuthorizationCredentials?> = data

    override suspend fun insertAccount(credentials: AuthorizationCredentials) {
        if (getAccountCount() != 0) throw IllegalStateException()
        dataStoreModule.updateData {
            copy(auth = credentials)
        }
    }

    override suspend fun logout() {
        dataStoreModule.updateData {
            copy(auth = null)
        }
    }

    override suspend fun getAccountCount(): Int = data.map { if (it != null) 1 else 0 }.first()
}