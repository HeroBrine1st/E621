package ru.herobrine1st.e621.ui.component.scaffold

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.herobrine1st.e621.util.PreviewUtils

class MainScaffoldState(
    val scaffoldState: ScaffoldState,
    val goToSettings: () -> Unit,
    val openBlacklistDialog: () -> Unit
)

/**
 * @param goToSettings called when user click "Settings". Should prohibit multiple Settings configurations in backstack.
 */
@Composable
fun rememberMainScaffoldState(
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    goToSettings: () -> Unit,
    openBlacklistDialog: () -> Unit
): MainScaffoldState {
    return remember {
        MainScaffoldState(
            ScaffoldState(drawerState, snackbarHostState),
            goToSettings,
            openBlacklistDialog
        )
    }
}

@Composable
fun MainScaffoldState.eraseSnackbarHostState() = rememberMainScaffoldState(
    drawerState = scaffoldState.drawerState,
    goToSettings = goToSettings,
    openBlacklistDialog = openBlacklistDialog
)

@PreviewUtils
@Composable
fun rememberPreviewMainScaffoldState() =
    rememberMainScaffoldState(
        goToSettings = {},
        openBlacklistDialog = {}
    )