/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2025 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.database.entity.BlacklistEntry

@Dao
interface BlacklistDao {
    @Query("SELECT COUNT(*) FROM blacklist")
    suspend fun count(): Int

    @Query("SELECT * FROM blacklist")
    suspend fun getAll(): List<BlacklistEntry>

    @Query("SELECT * FROM blacklist")
    fun getFlowOfAll(): Flow<List<BlacklistEntry>>

    @Query("DELETE FROM blacklist WHERE id=:id")
    suspend fun delete(id: Long)

    @Delete
    suspend fun delete(entry: BlacklistEntry)

    @Insert
    suspend fun insert(blacklist: BlacklistEntry): Long

    @Insert
    suspend fun insertAll(entries: List<BlacklistEntry>)

    @Update
    suspend fun update(blacklist: BlacklistEntry)

    @Transaction
    suspend fun update(entries: List<BlacklistEntry>) {
        entries.forEach {
            update(it)
        }
    }

    @Query("DELETE FROM blacklist")
    suspend fun clear()
}