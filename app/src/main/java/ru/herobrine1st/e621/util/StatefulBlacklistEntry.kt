@file:Suppress("MemberVisibilityCanBePrivate")

package ru.herobrine1st.e621.util

import androidx.compose.runtime.*
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.entity.BlacklistEntry

@Stable
class StatefulBlacklistEntry private constructor(query: String, enabled: Boolean, id: Long) {
    companion object {
        fun of(blacklistEntry: BlacklistEntry): StatefulBlacklistEntry =
            StatefulBlacklistEntry(blacklistEntry.query, blacklistEntry.enabled, blacklistEntry.id)

        fun create(query: String, enabled: Boolean = true): StatefulBlacklistEntry =
            StatefulBlacklistEntry(query, enabled, 0)
    }

    // UI state
    var query by mutableStateOf(query)
    var enabled by mutableStateOf(enabled)

    // Actual database state
    var id by mutableStateOf(id)
        private set
    var dbEnabled by mutableStateOf(enabled)
        private set
    var dbQuery by mutableStateOf(query)
        private set


    val isToggled get() = enabled != dbEnabled
    val isQueryChanged get() = query != dbQuery
    var isPendingDeletion by mutableStateOf(false)
        private set
    val isPendingInsertion get() = id == 0L
    val isPendingUpdate get() = isToggled || isQueryChanged
    val isChanged get() = isPendingInsertion || isPendingUpdate || isPendingDeletion


    fun resetChanges() {
        if (!isChanged) return
        query = dbQuery
        enabled = dbEnabled
        isPendingDeletion = false
    }


    fun markAsDeleted(deleted: Boolean = true) {
        assert(id != 0L)
        isPendingDeletion = deleted
    }

    val predicate by derivedStateOf { createTagProcessor(dbQuery) }

    fun applyInternalChanges(id: Long = 0L) {
        if (id != 0L) {
            if (isPendingInsertion) this.id = id
            else throw RuntimeException("ID is provided but there should be no insertion")
        }
        dbQuery = query
        dbEnabled = enabled
    }

}

fun BlacklistEntry.asStateful() = StatefulBlacklistEntry.of(this)

suspend fun BlacklistRepository.getAllEntriesAsStateful() = getAllEntries().map { it.asStateful() }