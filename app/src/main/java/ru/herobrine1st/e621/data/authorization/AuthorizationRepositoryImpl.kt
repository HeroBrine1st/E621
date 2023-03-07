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