package ru.herobrine1st.e621.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


// There should be only one
@Entity
data class Auth(
    @ColumnInfo(name = "login") val login: String,
    @ColumnInfo(name = "api_key") val apiKey: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0
)
