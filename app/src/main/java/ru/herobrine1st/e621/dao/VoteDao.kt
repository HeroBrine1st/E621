/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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