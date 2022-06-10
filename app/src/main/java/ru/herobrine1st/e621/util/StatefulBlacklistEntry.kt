@file:Suppress("MemberVisibilityCanBePrivate")

package ru.herobrine1st.e621.util

import androidx.compose.runtime.*
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.dao.BlacklistDao
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.database.Database
import ru.herobrine1st.e621.entity.BlacklistEntry

@Stable
class StatefulBlacklistEntry private constructor(query: String, enabled: Boolean, id: Long) {
    companion object {
        fun of(blacklistEntry: BlacklistEntry): StatefulBlacklistEntry =
            StatefulBlacklistEntry(blacklistEntry.query, blacklistEntry.enabled, blacklistEntry.id)

        fun create(query: String, enabled: Boolean = true): StatefulBlacklistEntry =
            StatefulBlacklistEntry(query, enabled, 0)
    }

    var id by mutableStateOf(id)
        private set
    var query by mutableStateOf(query)
    var enabled by mutableStateOf(enabled)


    // Actual database state
    private var dbEntryEnabled by mutableStateOf(enabled)
    private var dbEntryQuery by mutableStateOf(query)


    val isToggled get() = enabled != dbEntryEnabled
    val isQueryChanged get() = query != dbEntryQuery
    var isPendingDeletion by mutableStateOf(false)
        private set
    val isPendingInsertion get() = id == 0L
    val isPendingUpdate get() = isToggled || isQueryChanged
    val isChanged get() = isPendingInsertion || isPendingUpdate || isPendingDeletion


    fun resetChanges() {
        if (!isChanged) return
        query = dbEntryQuery
        enabled = dbEntryEnabled
        isPendingDeletion = false
    }


    fun markAsDeleted(deleted: Boolean = true) {
        assert(id != 0L)
        isPendingDeletion = deleted
    }

    val predicate by derivedStateOf { createTagProcessor(dbEntryQuery) }

    private suspend fun createDatabaseRecord(database: Database) {
        assert(id == 0L)
        id = database.blacklistDao().insert(
            BlacklistEntry(
                query = query,
                enabled = enabled
            )
        )
        dbEntryQuery = query
        dbEntryEnabled = enabled
    }

    private suspend fun updateDatabaseRecord(database: Database) {
        try {
            database.blacklistDao().update(
                BlacklistEntry(
                    id = id,
                    query = query,
                    enabled = enabled
                )
            )
        } catch (e: Throwable) { // Undo
            resetChanges()
            throw e
        }
        dbEntryQuery = query
        dbEntryEnabled = enabled
    }

    private suspend fun deleteDatabaseRecord(database: Database) {
        assert(id != 0L)
        try {
            database.blacklistDao().delete(id)
        } catch (e: Exception) { // Undo
            resetChanges()
            throw e
        }
    }

    suspend fun applyChanges(database: Database) {
        when {
            isPendingInsertion -> createDatabaseRecord(database)
            isPendingDeletion -> deleteDatabaseRecord(database)
            else -> updateDatabaseRecord(database)
        }
    }
}

fun BlacklistEntry.asStateful() = StatefulBlacklistEntry.of(this)

suspend fun BlacklistDao.getAllAsStateful() = getAll().map { it.asStateful() }

suspend fun StatefulBlacklistEntry.applyChanges(repository: BlacklistRepository) {
    when {
        isPendingInsertion -> repository.insertEntry(
            BlacklistEntry(
                id = id,
                query = query,
                enabled = enabled
            )
        )
        isPendingDeletion -> repository.deleteEntryById(id)
        isPendingUpdate -> repository.updateEntry(
            BlacklistEntry(
                id = id,
                query = query,
                enabled = enabled
            )
        )
    }
}