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

package ru.herobrine1st.e621.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import ru.herobrine1st.e621.database.dao.BlacklistDao
import ru.herobrine1st.e621.database.dao.VoteDao
import ru.herobrine1st.e621.database.entity.BlacklistEntry
import ru.herobrine1st.e621.database.entity.Vote

private const val VERSION = 3

@Database(
    entities = [BlacklistEntry::class, Vote::class],
    version = VERSION,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = Version2To3DeleteTableAuth::class)
    ]
)
abstract class Database : RoomDatabase() {
    abstract fun blacklistDao(): BlacklistDao
    abstract fun voteDao(): VoteDao
}
