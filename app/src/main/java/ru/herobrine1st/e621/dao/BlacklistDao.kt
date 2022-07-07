package ru.herobrine1st.e621.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.entity.BlacklistEntry

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