package ru.herobrine1st.e621.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.entity.Auth

@Dao
interface AuthDao {

    @Query("SELECT * FROM auth LIMIT 1")
    suspend fun get(): Auth?

    @Query("SELECT * FROM auth LIMIT 1")
    fun getFlow(): Flow<Auth?>

    @Insert
    suspend fun insert(auth: Auth): Long

    @Query("DELETE FROM auth")
    suspend fun deleteAll(): Int

    @Query("SELECT COUNT(*) FROM auth")
    suspend fun count(): Int
}