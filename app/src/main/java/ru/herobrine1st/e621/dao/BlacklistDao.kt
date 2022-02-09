package ru.herobrine1st.e621.dao

import androidx.room.*
import ru.herobrine1st.e621.entity.Blacklist

@Dao
interface BlacklistDao {
    @Query("SELECT COUNT(*) FROM blacklist")
    suspend fun count(): Int

    @Query("SELECT * FROM blacklist")
    suspend fun getAll(): Array<Blacklist>

    @Delete
    suspend fun delete(blacklist: Blacklist)

    @Insert
    suspend fun insert(blacklist: Blacklist)

    @Update
    suspend fun update(blacklist: Blacklist)

    @Query("DELETE FROM blacklist")
    suspend fun clear()
}