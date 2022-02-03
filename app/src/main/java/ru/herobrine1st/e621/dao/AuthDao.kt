package ru.herobrine1st.e621.dao

import androidx.room.*
import ru.herobrine1st.e621.entity.Auth

@Dao
interface AuthDao {

    @Query("SELECT * FROM auth LIMIT 1")
    suspend fun get(): Auth?

    @Insert
    suspend fun insert(auth: Auth)

    @Delete
    suspend fun delete(user: Auth)

    @Update
    suspend fun update(user: Auth)
}