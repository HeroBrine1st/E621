package ru.herobrine1st.e621.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blacklist")
data class BlacklistEntry(
    @ColumnInfo val query: String,
    @ColumnInfo val enabled: Boolean,
    @PrimaryKey(autoGenerate = true) val id: Long = 0
)