package ru.herobrine1st.e621.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blacklist")
class BlacklistEntry(
    @ColumnInfo(name = "query") var query: String,
    @ColumnInfo(name = "enabled") var enabled: Boolean,
    @PrimaryKey(autoGenerate = true) var id: Long = 0
)