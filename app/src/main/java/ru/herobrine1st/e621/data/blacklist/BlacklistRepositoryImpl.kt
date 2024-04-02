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

package ru.herobrine1st.e621.data.blacklist

import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.dao.BlacklistDao
import ru.herobrine1st.e621.data.BaseRepositoryImpl
import ru.herobrine1st.e621.database.Database
import ru.herobrine1st.e621.entity.BlacklistEntry

class BlacklistRepositoryImpl(
    database: Database,
    private val dao: BlacklistDao
) : BaseRepositoryImpl(database), BlacklistRepository {

    override fun getEntriesFlow(): Flow<List<BlacklistEntry>> = dao.getFlowOfAll()

    override suspend fun getAllEntries(): List<BlacklistEntry> = dao.getAll()

    override suspend fun updateEntry(entry: BlacklistEntry) = dao.update(entry)

    override suspend fun insertEntry(entry: BlacklistEntry) = dao.insert(entry)

    override suspend fun insertEntries(entries: List<BlacklistEntry>) = dao.insertAll(entries)

    override suspend fun deleteEntry(entry: BlacklistEntry) = dao.delete(entry)

    override suspend fun deleteEntryById(id: Long) = dao.delete(id)

    override suspend fun updateEntries(entries: List<BlacklistEntry>) = dao.update(entries)
    override suspend fun count(): Int = dao.count()
}