package ru.herobrine1st.e621.dao

import androidx.room.*
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.util.StatefulBlacklistEntry

@Dao
interface BlacklistDao {
    @Query("SELECT COUNT(*) FROM blacklist")
    suspend fun count(): Int

    @Query("SELECT * FROM blacklist")
    suspend fun getAll(): Array<BlacklistEntry>

    @Delete
    suspend fun delete(blacklist: BlacklistEntry)

    @Insert
    suspend fun insert(blacklist: BlacklistEntry)

    @Update
    suspend fun update(blacklist: BlacklistEntry)

    @Query("DELETE FROM blacklist")
    suspend fun clear()

    suspend fun getAllAsStateful() = getAll().map { StatefulBlacklistEntry.of(it) }
}