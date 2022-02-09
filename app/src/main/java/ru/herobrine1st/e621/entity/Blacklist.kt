package ru.herobrine1st.e621.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Blacklist(
    @ColumnInfo(name = "query") var query: String,
    @ColumnInfo(name = "enabled") var enabled: Boolean,
    @PrimaryKey(autoGenerate = true) val id: Int = -1
)