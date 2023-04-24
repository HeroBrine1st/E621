package ru.herobrine1st.e621.navigation.component.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.decompose.router.stack.pop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.navigation.LifecycleScope

class SettingsBlacklistEntryComponent(
    componentContext: ComponentContext,
    val id: Long,
    initialQuery: String,
    initialEnabled: Boolean,
    private val blacklistRepository: BlacklistRepository,
    private val navigator: StackNavigator<*>
) : ComponentContext by componentContext {

    var query by mutableStateOf(initialQuery)
    var enabled by mutableStateOf(initialEnabled)

    private val lifecycleScope = LifecycleScope()

    fun apply(callback: () -> Unit) {
        lifecycleScope.launch {
            if (id != 0L) blacklistRepository.updateEntry(
                BlacklistEntry(
                    id = id,
                    query = query,
                    enabled = enabled
                )
            )
            else blacklistRepository.insertEntry(
                BlacklistEntry(
                    query, enabled
                )
            )
            withContext(Dispatchers.Main.immediate) {
                callback()
                navigator.pop()
            }
        }
    }
}