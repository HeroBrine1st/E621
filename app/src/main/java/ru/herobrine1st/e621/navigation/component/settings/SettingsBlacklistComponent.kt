package ru.herobrine1st.e621.navigation.component.settings

import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.statekeeper.consume
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter

// This legacy code is what I simply don't wanna touch
// It works, it does complicated work, it is isolated and does not pollute other components - that's a good combination for a bad code.
// Refactoring is necessary to improve readability, but otherwise it just works

private const val STATE_KEY = "SETTINGS_BLACKLIST_COMPONENT_STATE_KEY"

class SettingsBlacklistComponent(
    private val blacklistRepository: BlacklistRepository,
    private val snackbar: SnackbarAdapter,
    componentContext: ComponentContext
) : ComponentContext by componentContext {
    // TODO use MutableStateList
    //      Do not forget to set derivedStateOf on "isBlacklistUpdated"
    //      Be aware this change will involve refactoring of UI too
    //      .
    //      I came up with better idea: this component only toggles entries and Room is the source of truth
    //      and this will reduce the level of buffering overhead
    //      Editing will be on secondary screen, where UI or component is the source of truth,
    //      but - luckily - that's only one entry, so that no buffering overhead because no lists!
    //      That's out of refactoring, leaving it to future from branch refactor/decompose
    // Also, the idea with secondary screen is great in terms of fancy UI
    private val _entriesFlow = MutableStateFlow<List<EditedBlacklistEntry>?>(null)
    val entriesFlow = _entriesFlow.asStateFlow()

    private val lifecycleScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        stateKeeper.register(STATE_KEY) {
            State(entries = _entriesFlow.value)
        }
        stateKeeper.consume<State>(STATE_KEY)?.let {
            _entriesFlow.value = it.entries
        }
        lifecycle.doOnDestroy {
            lifecycleScope.cancel()
        }
        lifecycle.doOnCreate {
            lifecycleScope.launch {
                // Changes from BlacklistTogglesDialog are ignored, but else configuration change = reset
                // With that, even process death cannot reset changes, which is better than it were.
                if (_entriesFlow.value == null)
                    _entriesFlow.value = blacklistRepository.getEntriesFlow().map { list ->
                        list.map {
                            EditedBlacklistEntry.from(it)
                        }
                    }.first()
            }
        }
    }

    var isUpdating by mutableStateOf(false)
        private set

    fun applyChanges() {
        isUpdating = true
        var wasError = false
        lifecycleScope.launch {
            blacklistRepository.withTransaction {
                _entriesFlow.value = _entriesFlow.value?.mapNotNull { entry ->
                    try {
                        when {
                            entry.isPendingInsertion -> entry.copy(
                                backingEntry = BlacklistEntry(
                                    query = entry.query,
                                    enabled = entry.enabled,
                                    id = blacklistRepository.insertEntry(entry.toEntry())
                                )
                            )
                            entry.isPendingUpdate -> {
                                blacklistRepository.updateEntry(entry.toEntry())
                                entry.copy(
                                    backingEntry = entry.backingEntry!!.copy(
                                        query = entry.query,
                                        enabled = entry.enabled
                                    )
                                )
                            }
                            entry.isPendingDeletion -> {
                                blacklistRepository.deleteEntryById(entry.id!!)
                                null
                            }
                            else -> entry
                        }
                    } catch (t: Throwable) {
                        Log.e(
                            TAG,
                            "Unknown error occurred while trying to update/insert/delete blacklist entry",
                            t
                        )
                        if (!wasError) {
                            snackbar.enqueueMessage( // Likely database in release and something else in tests
                                R.string.database_error_updating_blacklist,
                                SnackbarDuration.Long,
                                entry.query
                            )
                            wasError = true
                        }
                        entry.reset()
                    }
                }
            }
            isUpdating = false
        }
    }

    fun appendEntry(query: String) {
        _entriesFlow.value = _entriesFlow.value?.plus(EditedBlacklistEntry(query))
    }

    fun resetEntry(entry: EditedBlacklistEntry) {
        if (entry.isPendingInsertion) {
            _entriesFlow.value = _entriesFlow.value?.minus(entry)
        } else {
            _entriesFlow.value = _entriesFlow.value?.mapNotNull {
                if (it == entry) entry.reset() else it
            }
        }
    }

    fun markEntryAsDeleted(entry: EditedBlacklistEntry, deleted: Boolean = true) {
        if (entry.isPendingInsertion) {
            _entriesFlow.value = _entriesFlow.value?.minus(entry)
        } else {
            _entriesFlow.value = _entriesFlow.value
                ?.map {
                    if (it == entry) entry.copy(pendingDeletion = deleted) else it
                }
        }
    }

    fun resetChanges() {
        _entriesFlow.value =
            _entriesFlow.value?.mapNotNull { it.reset() }
    }

    fun editEntry(
        entry: EditedBlacklistEntry,
        query: String = entry.query,
        enabled: Boolean = entry.enabled
    ) {
        _entriesFlow.value = _entriesFlow.value?.map {
            if (it == entry) entry.copy(query = query, enabled = enabled) else it
        }
    }

    companion object {
        const val TAG = "SettingsBlacklistComponent"
    }

    @Parcelize
    private data class State(
        val entries: List<EditedBlacklistEntry>?
    ) : Parcelable
}

@Immutable
@Parcelize
data class EditedBlacklistEntry(
    val query: String,
    val enabled: Boolean = true,
    val pendingDeletion: Boolean = false,
    val backingEntry: BlacklistEntry? = null
) : Parcelable {
    val isPendingInsertion get() = backingEntry == null
    val isPendingUpdate get() = backingEntry != null && (query != backingEntry.query || enabled != backingEntry.enabled)
    val isPendingDeletion get() = backingEntry != null && pendingDeletion
    val isChanged get() = backingEntry == null || query != backingEntry.query || enabled != backingEntry.enabled || pendingDeletion

    val id get() = backingEntry?.id

    fun reset() = backingEntry?.let { from(it) }

    fun toEntry(): BlacklistEntry = BlacklistEntry(
        query = query,
        enabled = enabled,
        id = backingEntry?.id ?: 0
    )

    companion object {
        fun from(blacklistEntry: BlacklistEntry) = EditedBlacklistEntry(
            blacklistEntry.query,
            blacklistEntry.enabled,
            false,
            blacklistEntry
        )
    }
}