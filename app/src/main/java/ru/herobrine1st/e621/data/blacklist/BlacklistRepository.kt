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

package ru.herobrine1st.e621.data.blacklist

import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.data.BaseRepository
import ru.herobrine1st.e621.entity.BlacklistEntry

interface BlacklistRepository: BaseRepository {
    fun getEntriesFlow(): Flow<List<BlacklistEntry>>

    suspend fun getAllEntries(): List<BlacklistEntry>

    suspend fun updateEntry(entry: BlacklistEntry)

    suspend fun insertEntry(entry: BlacklistEntry): Long

    suspend fun insertEntries(entries: List<BlacklistEntry>)

    suspend fun deleteEntry(entry: BlacklistEntry)

    suspend fun deleteEntryById(id: Long)

    suspend fun updateEntries(entries: List<BlacklistEntry>)

    suspend fun count(): Int
}