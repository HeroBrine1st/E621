package ru.herobrine1st.e621.navigation.component.settings

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.push
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter

class SettingsBlacklistComponent(
    private val blacklistRepository: BlacklistRepository,
    private val snackbar: SnackbarAdapter,
    private val navigator: StackNavigator<Config>,
    componentContext: ComponentContext
) : ComponentContext by componentContext {
    private val lifecycleScope = LifecycleScope()

    val entriesFlow: StateFlow<List<BlacklistEntry>?> = blacklistRepository.getEntriesFlow()
        .runningFold(mutableStateListOf<BlacklistEntry>()) { acc, value ->
            acc.clear()
            acc.addAll(value)
            acc
        } // UI sees the same list
        .drop(1) // Drop initial empty list
        .stateIn(lifecycleScope, SharingStarted.Eagerly, initialValue = null)

    var isUpdating by mutableStateOf(false)
        private set

    fun toggleEntry(
        blacklistEntry: BlacklistEntry,
        tooLongMs: Long = 125L,
        onTooLong: () -> Unit,
        onComplete: () -> Unit
    ) {
        val job = lifecycleScope.launch {
            delay(tooLongMs)
            onTooLong()
        }
        lifecycleScope.launch {
            try {
                blacklistRepository.updateEntry(blacklistEntry.copy(enabled = !blacklistEntry.enabled))
                // Wait for update from blacklist
                entriesFlow.first { blacklistEntries ->
                    // TODO do something with linear complexity but also keep order
                    //      list is sorted by id, but I don't remember guarantees
                    //      binary search may be useful
                    blacklistEntries?.any { it.id == blacklistEntry.id && it.enabled == !blacklistEntry.enabled } != true
                }
            } catch (t: Throwable) {
                snackbar.enqueueMessage(R.string.database_error_updating_blacklist)
            }
            job.cancel()
            onComplete()
        }
    }

    fun createNewEntry() {
        navigator.navigate { configList ->
            if (configList.last() is Config.Settings.Blacklist.Entry) {
                Log.w(TAG, "Prevented duplicated config (user double clicked a button?)")
                return@navigate configList
            }
            configList + Config.Settings.Blacklist.Entry(
                id = 0,
                query = "",
                enabled = true
            )
        }
    }

    fun deleteEntry(entry: BlacklistEntry, callback: () -> Unit) {
        lifecycleScope.launch {
            blacklistRepository.deleteEntry(entry)
            callback()
        }
    }

    fun editEntry(entry: BlacklistEntry) {
        navigator.push(
            Config.Settings.Blacklist.Entry(
                id = entry.id,
                query = entry.query,
                enabled = entry.enabled
            )
        )
    }

    companion object {
        const val TAG = "SettingsBlacklistComponent"
    }
}