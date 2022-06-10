package ru.herobrine1st.e621.data.blacklist

import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.dao.BlacklistDao
import ru.herobrine1st.e621.entity.BlacklistEntry
import javax.inject.Inject

class BlacklistRepositoryImpl @Inject constructor(val dao: BlacklistDao) : BlacklistRepository {
    override fun getEntriesFlow(): Flow<List<BlacklistEntry>> = dao.getFlow()

    override suspend fun getAllEntries(): List<BlacklistEntry> = dao.getAll()

    override suspend fun updateEntry(entry: BlacklistEntry) = dao.update(entry)

    override suspend fun insertEntry(entry: BlacklistEntry) = dao.insert(entry)

    override suspend fun deleteEntry(entry: BlacklistEntry) = dao.delete(entry)

    override suspend fun deleteEntryById(id: Long) = dao.delete(id)

    override suspend fun updateEntries(entries: List<BlacklistEntry>) = dao.update(entries)
}