@file:Suppress("MemberVisibilityCanBePrivate")

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

        fun create(query: String, enabled: Boolean = true): StatefulBlacklistEntry =
            StatefulBlacklistEntry(BlacklistEntry(query = query, enabled))
    }

    var query by mutableStateOf(dbEntry.query)
    var enabled by mutableStateOf(dbEntry.enabled)

    // Make them stateful so that database update will trigger recomposition
    private var dbEntryEnabled by mutableStateOf(dbEntry.enabled)
    private var dbEntryQuery by mutableStateOf(dbEntry.query)
    private var dbEntryId by mutableStateOf(dbEntry.id)

    var pendingDeletion by mutableStateOf(false)
        private set

    fun isToggled() = enabled != dbEntryEnabled
    fun isQueryChanged() = query != dbEntryQuery
    fun isPendingDeletion() = pendingDeletion
    fun isPendingInsertion() = dbEntryId == 0L
    fun isPendingUpdate() = isToggled() || isQueryChanged()
    fun isChanged() = isPendingInsertion() || isPendingUpdate() || isPendingDeletion()


    fun resetChanges() {
        query = dbEntry.query; dbEntryQuery = dbEntryQuery
        enabled = dbEntry.enabled; dbEntryEnabled = dbEntryEnabled
        pendingDeletion = false
    }


    fun markAsDeleted(deleted: Boolean = true) {
        assert(dbEntry.id != 0L)
        pendingDeletion = deleted
    }

    val predicate by derivedStateOf { createTagProcessor(query) }

    private suspend fun createDatabaseRecord(database: Database) {
        assert(dbEntry.id == 0L)
        dbEntry.query = query
        dbEntry.enabled = enabled
        dbEntry.id = database.blacklistDao().insert(dbEntry)
        dbEntryQuery = query
        dbEntryEnabled = dbEntryEnabled
        dbEntryId = dbEntry.id
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
        dbEntryEnabled = enabled
        dbEntryQuery = query
        dbEntryId = dbEntry.id
    }

    private suspend fun deleteDatabaseRecord(database: Database) {
        assert(dbEntry.id != 0L)
        try {
            database.blacklistDao().delete(dbEntry)
        } catch (e: Exception) { // Undo
            resetChanges()
            throw e
        }
    }

    suspend fun applyChanges(database: Database) {
        when {
            dbEntry.id == 0L -> createDatabaseRecord(database)
            pendingDeletion -> deleteDatabaseRecord(database)
            else -> updateDatabaseRecord(database)
        }
    }
}