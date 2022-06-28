package ru.herobrine1st.e621.util

import androidx.compose.material.SnackbarDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthorizationNotifier @Inject constructor(
    val snackbarAdapter: SnackbarAdapter
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    fun notifyAuthorizationRevoked() {
        coroutineScope.launch {
            snackbarAdapter.enqueueMessage(R.string.auth_revoked, SnackbarDuration.Indefinite)
        }
    }
}