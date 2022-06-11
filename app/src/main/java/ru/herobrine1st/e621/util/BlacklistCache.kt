package ru.herobrine1st.e621.util

import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.scopes.ActivityRetainedScoped
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import javax.inject.Inject

/**
 * Caching source of blacklist entries and precompiled predicates
 */
@ActivityRetainedScoped
class BlacklistCache @Inject constructor(
    private val repository: BlacklistRepository,
    private val snackbar: SnackbarAdapter
) {
    var isLoading by mutableStateOf(true)
        private set

    private val _entries = mutableStateListOf<StatefulBlacklistEntry>()

    val entries = _entries as List<StatefulBlacklistEntry>

    suspend fun init() {
        if (!isLoading) return
        _entries.addAll(repository.getAllEntriesAsStateful())
        isLoading = false
    }

    suspend fun applyChanges() {
        val iterator = _entries.listIterator()

        var wasError = false
        while (iterator.hasNext()) {
            val entry = iterator.next()
            try {
                when {
                    entry.isPendingInsertion -> {
                        val id = repository.insertEntry(
                            BlacklistEntry(
                                entry.query,
                                entry.enabled,
                                entry.id
                            ).debug {
                                Log.d(TAG, "Inserting $this")
                            }
                        )
                        entry.applyInternalChanges(id)
                    }
                    entry.isPendingDeletion -> {
                        Log.d(TAG, "Deleting ${entry.query}")
                        repository.deleteEntryById(entry.id)
                        iterator.remove()
                    }
                    entry.isPendingUpdate -> {
                        repository.updateEntry(
                            BlacklistEntry(
                                entry.query,
                                entry.enabled,
                                entry.id
                            ).debug {
                                Log.d(TAG, "Updating $this")
                            }
                        )
                        entry.applyInternalChanges()
                    }
                }
            } catch (e: Throwable) {
                // Try to do as much as possible, undoing individual changes on errors
                if (entry.isPendingInsertion) iterator.remove() else entry.resetChanges()
                Log.e(
                    TAG,
                    "Unknown error occurred while trying to update/insert/delete blacklist entry",
                    e
                )
                if (!wasError) {
                    snackbar.enqueueMessage( // Likely database in release and something else in tests
                        R.string.database_error_updating_blacklist,
                        SnackbarDuration.Long,
                        entry.query
                    )
                    wasError = true
                }
            }
        }
    }

    fun appendEntry(query: String) = StatefulBlacklistEntry.create(query).also { _entries.add(it) }


    fun removeEntry(entry: StatefulBlacklistEntry) {
        _entries.remove(entry)
    }

    fun markEntryAsDeleted(entry: StatefulBlacklistEntry, deleted: Boolean = true) {
        if (entry.isPendingInsertion) removeEntry(entry)
        else entry.markAsDeleted(deleted)
    }

    fun resetEntry(entry: StatefulBlacklistEntry) {
        if (entry.isPendingInsertion) removeEntry(entry)
        else entry.resetChanges()
    }

    fun resetChanges() {
        _entries.removeIf { it.isPendingInsertion }
        _entries.forEach { it.resetChanges() }
    }

    companion object {
        const val TAG = "BlacklistCache"
    }
}
