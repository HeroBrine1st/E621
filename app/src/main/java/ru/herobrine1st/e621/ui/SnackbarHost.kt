package ru.herobrine1st.e621.ui

import androidx.annotation.StringRes
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableSharedFlow
import ru.herobrine1st.e621.R

data class SnackbarMessage(
    @StringRes val stringId: Int,
    val duration: SnackbarDuration,
    val formatArgs: Array<out Any>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SnackbarMessage

        if (stringId != other.stringId) return false
        if (duration != other.duration) return false
        if (!formatArgs.contentEquals(other.formatArgs)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stringId
        result = 31 * result + duration.hashCode()
        result = 31 * result + formatArgs.contentHashCode()
        return result
    }
}

@Composable
fun SnackbarHost(
    snackbarMessagesFlow: MutableSharedFlow<SnackbarMessage>,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current

    LaunchedEffect(snackbarMessagesFlow, snackbarHostState) {
        snackbarMessagesFlow.collect {
            snackbarHostState.showSnackbar(
                context.resources.getString(it.stringId, *it.formatArgs),
                context.resources.getString(R.string.okay),
                it.duration
            )
        }
    }
}