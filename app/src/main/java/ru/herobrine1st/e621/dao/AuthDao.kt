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

    // Maybe one day there will be support for multiple accounts. Maybe.
    // This is very unlikely (who ever will use it? and, therefore, why should I code it?), but I design
    // this app with this feature in my mind. At least interceptor won't delete all accounts now.
    // Another question is, how there would be 2 or more accounts, but this is trivial
    // and left as an exercise for the reader
    @Delete
    suspend fun delete(auth: Auth)

    @Update
    suspend fun update(user: Auth)
}