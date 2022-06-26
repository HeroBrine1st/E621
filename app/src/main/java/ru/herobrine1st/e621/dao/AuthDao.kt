package ru.herobrine1st.e621.dao

import androidx.room.*
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
    suspend fun delete(): Int

    @Delete
    suspend fun delete(auth: Auth)

    @Update
    suspend fun update(user: Auth)
}