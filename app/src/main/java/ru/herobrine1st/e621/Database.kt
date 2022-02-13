package ru.herobrine1st.e621

import androidx.compose.runtime.compositionLocalOf
import androidx.room.Database
import androidx.room.RoomDatabase
import ru.herobrine1st.e621.dao.AuthDao
import ru.herobrine1st.e621.dao.BlacklistDao
import ru.herobrine1st.e621.entity.Auth
import ru.herobrine1st.e621.entity.BlacklistEntry

val LocalDatabase = compositionLocalOf<ru.herobrine1st.e621.Database> { error("No database found") }

@Database(entities = [Auth::class, BlacklistEntry::class], version = 1)
abstract class Database : RoomDatabase() {
    abstract fun authDao(): AuthDao
    abstract fun blacklistDao(): BlacklistDao
}
