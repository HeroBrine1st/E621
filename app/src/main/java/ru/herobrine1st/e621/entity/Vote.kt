package ru.herobrine1st.e621.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "votes",
    indices = [
        Index("postId", unique = true)
    ]
)
data class Vote(
    @ColumnInfo val postId: Int,
    @ColumnInfo var vote: Int,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
) {
    init {
        assert(vote in -1..1)
    }
}