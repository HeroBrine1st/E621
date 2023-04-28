package ru.herobrine1st.e621.ui.component.scaffold

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.herobrine1st.e621.util.PreviewUtils

class ScreenSharedState(
    val snackbarHostState: SnackbarHostState, // TODO maybe every screen should have its own snackbar?
    val goToSettings: () -> Unit,
    val openBlacklistDialog: () -> Unit
)

/**
 * @param goToSettings called when user click "Settings". Should prohibit multiple Settings configurations in backstack.
 */
@Composable
fun rememberScreenSharedState(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    goToSettings: () -> Unit,
    openBlacklistDialog: () -> Unit
): ScreenSharedState {
    return remember {
        ScreenSharedState(
            snackbarHostState,
            goToSettings,
            openBlacklistDialog
        )
    }
}

@PreviewUtils
@Composable
fun rememberScreenPreviewSharedState() =
    rememberScreenSharedState(
        goToSettings = {}
    ) {}