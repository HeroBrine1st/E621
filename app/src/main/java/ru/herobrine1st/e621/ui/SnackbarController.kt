package ru.herobrine1st.e621.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.R

@Composable
fun SnackbarController(applicationViewModel: ApplicationViewModel, scaffoldState: ScaffoldState){
    val coroutineScope = rememberCoroutineScope()
    val snackbarMessage = applicationViewModel.snackbarMessage
    if(snackbarMessage != null && !applicationViewModel.snackbarShowing) {
        val actionLabel = stringResource(R.string.okay)
        val message = stringResource(snackbarMessage.first)
        val duration = snackbarMessage.second
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