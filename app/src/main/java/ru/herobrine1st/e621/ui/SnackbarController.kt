package ru.herobrine1st.e621.ui

import androidx.annotation.StringRes
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.R

data class SnackbarMessage(@StringRes val stringId: Int, val duration: SnackbarDuration, val formatArgs: Array<out Any>) {
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
fun SnackbarController(applicationViewModel: ApplicationViewModel, scaffoldState: ScaffoldState){
    val coroutineScope = rememberCoroutineScope()
    val snackbarMessage = applicationViewModel.snackbarMessage
    if(snackbarMessage != null && !applicationViewModel.snackbarShowing) {
        val actionLabel = stringResource(R.string.okay)
        val message = stringResource(snackbarMessage.stringId, *snackbarMessage.formatArgs)
        val duration = snackbarMessage.duration
        LaunchedEffect(snackbarMessage) {
            coroutineScope.launch {
                if(applicationViewModel.snackbarShowing) return@launch
                applicationViewModel.notifySnackbarMessageWillDisplay()
                scaffoldState.snackbarHostState.showSnackbar(
                    message,
                    actionLabel,
                    duration
                )
                applicationViewModel.notifySnackbarMessageDisplayed()
            }
        }
    }
}