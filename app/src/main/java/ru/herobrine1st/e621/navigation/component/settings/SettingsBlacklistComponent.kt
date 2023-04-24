package ru.herobrine1st.e621.navigation.component.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter

// This legacy code is what I simply don't wanna touch
// It works, it does complicated work, it is isolated and does not pollute other components - that's a good combination for a bad code.
// Refactoring is necessary to improve readability, but otherwise it just works

private const val STATE_KEY = "SETTINGS_BLACKLIST_COMPONENT_STATE_KEY"

class SettingsBlacklistComponent(
    private val blacklistRepository: BlacklistRepository,
    private val snackbar: SnackbarAdapter,
    private val navigator: StackNavigator<Config>,
    componentContext: ComponentContext
) : ComponentContext by componentContext {
    private val lifecycleScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    // TODO every new update may lead to full composition
    //      there needs a difference-applying algorithm with MutableStateList
    val entriesFlow = blacklistRepository.getEntriesFlow()
        .stateIn(lifecycleScope, SharingStarted.Eagerly, initialValue = null)

    init {
        lifecycle.doOnDestroy {
            lifecycleScope.cancel()
        }
    }

    var isUpdating by mutableStateOf(false)
        private set

    fun toggleEntry(blacklistEntry: BlacklistEntry, callback: () -> Unit) {
        lifecycleScope.launch {
            try {
                blacklistRepository.updateEntry(blacklistEntry.copy(enabled = !blacklistEntry.enabled))
                // Wait for update from blacklist
                entriesFlow.first { blacklistEntries ->
                    // TODO do something with linear complexity but also keep order
                    blacklistEntries?.any { it.id == blacklistEntry.id && it.enabled == !blacklistEntry.enabled } != true
                }
            } catch (t: Throwable) {
                snackbar.enqueueMessage(R.string.database_error_updating_blacklist)
            }
            callback()
        }
    }

    fun createNewEntry() {
        navigator.push(
            Config.Settings.Blacklist.Entry(
                id = 0,
                query = "",
                enabled = true
            )
        )
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