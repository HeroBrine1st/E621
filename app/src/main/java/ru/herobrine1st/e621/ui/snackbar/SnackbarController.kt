package ru.herobrine1st.e621.ui.snackbar

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.e621.R


@Composable
fun SnackbarController(
    snackbarMessagesFlow: Flow<SnackbarMessage>,
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