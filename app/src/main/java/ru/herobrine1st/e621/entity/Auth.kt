package ru.herobrine1st.e621.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


// There should be only one
@Entity
data class Auth(
    @ColumnInfo(name = "login") var login: String,
    @ColumnInfo(name = "api_key") var apiKey: String,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)
