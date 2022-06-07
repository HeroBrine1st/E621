package ru.herobrine1st.e621.ui.snackbar

import androidx.annotation.StringRes
import androidx.compose.material.SnackbarDuration
import kotlinx.coroutines.flow.MutableSharedFlow

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
        if (!(formatArgs.contentEquals(other.formatArgs))) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stringId
        result = 31 * result + duration.hashCode()
        result = 31 * result + formatArgs.contentHashCode()
        return result
    }
}

suspend fun MutableSharedFlow<SnackbarMessage>.enqueueMessage(
    @StringRes resourceId: Int,
    duration: SnackbarDuration = SnackbarDuration.Long,
    vararg formatArgs: Any
) {
    this.emit(
        SnackbarMessage(resourceId, duration, formatArgs)
    )
}