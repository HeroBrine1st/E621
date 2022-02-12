package ru.herobrine1st.e621.util

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.herobrine1st.e621.Database
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.entity.BlacklistEntry

class StatefulBlacklistEntry private constructor(private val dbEntry: BlacklistEntry) {
    companion object {
        fun of(blacklistEntry: BlacklistEntry): StatefulBlacklistEntry =
            StatefulBlacklistEntry(blacklistEntry)

        fun create(query: String): StatefulBlacklistEntry =
            StatefulBlacklistEntry(BlacklistEntry(query = query, enabled = true))
    }
    var query by mutableStateOf(dbEntry.query)
    var enabled by mutableStateOf(dbEntry.enabled)

    var pendingDeletion by mutableStateOf(false)
        private set

    fun isToggled() = enabled != dbEntry.enabled
    fun isQueryChanged() = query != dbEntry.query
    fun isPendingDeletion() = pendingDeletion // maybe won't work
    fun isPendingInsertion() = dbEntry.id == 0
    fun isPendingUpdate() = isToggled() || isQueryChanged()
    fun isChanged() = isPendingInsertion() || isPendingUpdate() || isPendingDeletion()


    fun resetChanges() {
        query = dbEntry.query
        enabled = dbEntry.enabled
        pendingDeletion = false
    }

    fun markAsDeleted() {
        assert(!isPendingInsertion())
        pendingDeletion = true
    }

    val predicate by derivedStateOf { createTagProcessor(query) }

    private suspend fun createDatabaseRecord(database: Database) {
        assert(isPendingInsertion())
        dbEntry.query = query
        dbEntry.enabled = enabled
        database.blacklistDao().insert(dbEntry)
    }

    private suspend fun updateDatabaseRecord(database: Database) {
        val oldQuery = dbEntry.query
        val oldEnabled = dbEntry.enabled
        dbEntry.query = query
        dbEntry.enabled = enabled
        try {
            database.blacklistDao().update(dbEntry)
        } catch (e: Throwable) { // Undo
            dbEntry.query = oldQuery
            dbEntry.enabled = oldEnabled
            query = oldQuery
            enabled = oldEnabled
            throw e
        }
    }

    private suspend fun deleteDatabaseRecord(database: Database) {
        assert(dbEntry.id != 0)
        try {
            database.blacklistDao().delete(dbEntry)
        } catch (e: Exception) { // Undo
            resetChanges()
            throw e
        }
    }

    suspend fun applyChanges(database: Database) {
        when {
            dbEntry.id == 0 -> createDatabaseRecord(database)
            pendingDeletion -> deleteDatabaseRecord(database)
            else -> updateDatabaseRecord(database)
        }
    }
}