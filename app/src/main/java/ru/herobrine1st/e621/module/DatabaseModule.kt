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

package ru.herobrine1st.e621.module

import android.content.Context
import androidx.room.Room
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.database.Database
import ru.herobrine1st.e621.database.repository.blacklist.BlacklistRepositoryImpl
import ru.herobrine1st.e621.database.repository.vote.VoteRepositoryImpl

class DatabaseModule(applicationContext: Context) {

    val database by lazy {
        Room.databaseBuilder<Database>(
            applicationContext,
            BuildConfig.DATABASE_NAME,
            factory = { ru.herobrine1st.e621.database.Database_Impl() }
        ).build()
    }

    val blacklistRepository by lazy {
        BlacklistRepositoryImpl(database, database.blacklistDao())
    }

    val voteRepository by lazy {
        VoteRepositoryImpl(database, database.voteDao())
    }
}