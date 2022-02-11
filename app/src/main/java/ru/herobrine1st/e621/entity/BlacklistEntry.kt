package ru.herobrine1st.e621.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.herobrine1st.e621.api.createTagProcessor

@Entity(tableName = "blacklist")
class BlacklistEntry(
    @ColumnInfo(name = "query") var query: String,
    @ColumnInfo(name = "enabled") var enabled: Boolean,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
) {
    val predicate by lazy { createTagProcessor(query) }
}