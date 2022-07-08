@file:Suppress("MemberVisibilityCanBePrivate")

package ru.herobrine1st.e621.util

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.herobrine1st.e621.entity.BlacklistEntry

@Stable
class ToggleableBlacklistEntry private constructor(val query: String, val dbEnabled: Boolean, val id: Long) {
    companion object {
        fun of(blacklistEntry: BlacklistEntry): ToggleableBlacklistEntry =
            ToggleableBlacklistEntry(blacklistEntry.query, blacklistEntry.enabled, blacklistEntry.id)

        fun create(query: String, enabled: Boolean = true): ToggleableBlacklistEntry =
            ToggleableBlacklistEntry(query, enabled, 0)
    }

    var enabled by mutableStateOf(dbEnabled)

    val isChanged get() = enabled != dbEnabled


    fun resetChanges() {
        if (!isChanged) return
        enabled = dbEnabled
    }


    fun toEntry() = BlacklistEntry(query, enabled, id)
}

fun BlacklistEntry.asToggleable() = ToggleableBlacklistEntry.of(this)

