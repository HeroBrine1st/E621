package ru.herobrine1st.e621.dao

import androidx.room.*
import ru.herobrine1st.e621.entity.Vote

@Dao
interface VoteDao {
    @Query("SELECT (vote) FROM votes WHERE postId = :postId") // postId is unique
    suspend fun getVote(postId: Int): Int?

    @Query("SELECT * FROM votes WHERE postId = :postId")
    suspend fun get(postId: Int): Vote?

    @Update
    suspend fun update(vote: Vote)

    @Insert
    suspend fun insert(vote: Vote): Long

    @Delete
    suspend fun delete(vote: Vote)

    /**
     * Update row with given postId or create new if not found
     * @return true if updated, false if created
     */
    suspend fun insertOrUpdate(postId: Int, vote: Int): Boolean {
        val entry = get(postId)
        return if(entry != null) {
            entry.vote = vote
            update(entry)
            true
        } else {
            val newEntry = Vote(postId, vote)
            insert(newEntry)
            false
        }
    }
}