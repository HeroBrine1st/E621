package ru.herobrine1st.e621.ui.snackbar

import androidx.annotation.StringRes
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow

class SnackbarAdapter(private val snackbarSharedFlow: MutableSharedFlow<SnackbarMessage>) {
    suspend fun enqueueMessage(
        @StringRes resourceId: Int,
        duration: SnackbarDuration = SnackbarDuration.Long,
        vararg formatArgs: Any
    ) = snackbarSharedFlow.enqueueMessage(resourceId, duration, formatArgs)
}

val LocalSnackbar =
    staticCompositionLocalOf<SnackbarAdapter> { error("There's no snackbar adapter in this scope") }