package ru.herobrine1st.e621.ui.component.scaffold

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.herobrine1st.e621.util.PreviewUtils

class MainScaffoldState(
    val snackbarHostState: SnackbarHostState,
    val goToSettings: () -> Unit,
    val openBlacklistDialog: () -> Unit
)

/**
 * @param goToSettings called when user click "Settings". Should prohibit multiple Settings configurations in backstack.
 */
@Composable
fun rememberMainScaffoldState(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    goToSettings: () -> Unit,
    openBlacklistDialog: () -> Unit
): MainScaffoldState {
    return remember {
        MainScaffoldState(
            snackbarHostState,
            goToSettings,
            openBlacklistDialog
        )
    }
}

@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "Should be replaced with snackbarHost = {}",
    replaceWith = ReplaceWith("")
)
@Composable
fun MainScaffoldState.eraseSnackbarHostState() = rememberMainScaffoldState(
    goToSettings = goToSettings,
    openBlacklistDialog = openBlacklistDialog
)

@PreviewUtils
@Composable
fun rememberPreviewMainScaffoldState() =
    rememberMainScaffoldState(
        goToSettings = {}
    ) {}