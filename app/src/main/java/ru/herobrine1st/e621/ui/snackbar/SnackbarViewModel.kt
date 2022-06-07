package ru.herobrine1st.e621.ui.snackbar

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow

class SnackbarViewModel : ViewModel() {
    val snackbarMessagesFlow = MutableSharedFlow<SnackbarMessage>()
}