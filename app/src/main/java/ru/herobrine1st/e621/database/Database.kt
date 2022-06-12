package ru.herobrine1st.e621.database

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import ru.herobrine1st.e621.dao.AuthDao
import ru.herobrine1st.e621.dao.BlacklistDao
import ru.herobrine1st.e621.dao.VoteDao
import ru.herobrine1st.e621.entity.Auth
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.entity.Vote

val LocalDatabase = staticCompositionLocalOf<ru.herobrine1st.e621.database.Database> { error("No database found") }
private const val VERSION = 2

@Database(
    entities = [Auth::class, BlacklistEntry::class, Vote::class],
    version = VERSION,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
abstract class Database : RoomDatabase() {
    abstract fun authDao(): AuthDao
    abstract fun blacklistDao(): BlacklistDao
    abstract fun voteDao(): VoteDao
}
