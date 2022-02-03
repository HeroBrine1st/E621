package ru.herobrine1st.e621

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.herobrine1st.e621.dao.AuthDao
import ru.herobrine1st.e621.entity.Auth

@Database(entities = [Auth::class], version = 1, exportSchema = false)
abstract class Database : RoomDatabase() {
    abstract fun authDao(): AuthDao
}
