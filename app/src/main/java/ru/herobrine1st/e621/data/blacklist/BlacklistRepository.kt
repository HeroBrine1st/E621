package ru.herobrine1st.e621.data.blacklist

import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.entity.BlacklistEntry

interface BlacklistRepository {
    fun getAll(): Flow<List<BlacklistEntry>>

    suspend fun updateEntry(entry: BlacklistEntry)

    suspend fun insertEntry(entry: BlacklistEntry): Long

    suspend fun deleteEntry(entry: BlacklistEntry)
}