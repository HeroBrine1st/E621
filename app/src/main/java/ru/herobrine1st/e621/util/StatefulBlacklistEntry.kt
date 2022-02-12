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
        fun of(blacklistEntry: BlacklistEntry): StatefulBlacklistEntry {
            return StatefulBlacklistEntry(blacklistEntry)
        }
    }
    var query by mutableStateOf(dbEntry.query)
    var enabled by mutableStateOf(dbEntry.enabled)

    fun isToggled() = enabled != dbEntry.enabled
    fun isQueryChanged() = query != dbEntry.query
    fun isChanged() = isToggled() || isQueryChanged()

    fun resetChanges() {
        query = dbEntry.query
        enabled = dbEntry.enabled
    }

    val predicate by derivedStateOf { createTagProcessor(query) }

    suspend fun updateDatabaseRecord(database: Database) {
        val oldQuery = dbEntry.query
        val oldEnabled = dbEntry.enabled
        dbEntry.query = query
        dbEntry.enabled = enabled
        try {
            database.blacklistDao().update(dbEntry)
        } catch (e: Throwable) {
            dbEntry.query = oldQuery
            dbEntry.enabled = oldEnabled
            query = oldQuery
            enabled = oldEnabled
            throw e
        }
    }
}