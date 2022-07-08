package ru.herobrine1st.e621.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import ru.herobrine1st.e621.dao.BlacklistDao
import ru.herobrine1st.e621.dao.VoteDao
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.entity.Vote

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
