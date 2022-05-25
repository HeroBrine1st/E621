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

    @Query("DELETE FROM blacklist WHERE id=:id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun insert(blacklist: BlacklistEntry): Long

    @Update
    suspend fun update(blacklist: BlacklistEntry)

    @Query("DELETE FROM blacklist")
    suspend fun clear()
}