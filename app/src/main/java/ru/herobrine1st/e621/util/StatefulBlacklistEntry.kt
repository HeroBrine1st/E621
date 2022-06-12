@file:Suppress("MemberVisibilityCanBePrivate")

package ru.herobrine1st.e621.util

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    fun toEntry() = BlacklistEntry(
        query, enabled, id
    )
}

fun BlacklistEntry.asStateful() = StatefulBlacklistEntry.of(this)

