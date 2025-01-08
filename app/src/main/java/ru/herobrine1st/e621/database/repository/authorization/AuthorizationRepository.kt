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
import ru.herobrine1st.e621.preference.AuthorizationCredentials

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
    suspend fun insertAccount(credentials: AuthorizationCredentials)

    /**
     * Logs out from this session's credentials
     */
    suspend fun logout()

    /**
     * @return Available accounts count
     */
    suspend fun getAccountCount(): Int
}