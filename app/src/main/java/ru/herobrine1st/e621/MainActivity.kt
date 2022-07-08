package ru.herobrine1st.e621

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.exoplayer2.ExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.module.LocalExoPlayer
import ru.herobrine1st.e621.preference.getPreferencesAsState
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.preference.updatePreferences
import ru.herobrine1st.e621.ui.MainScaffold
import ru.herobrine1st.e621.ui.dialog.BlacklistTogglesDialog
import ru.herobrine1st.e621.ui.snackbar.LocalSnackbar
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.ui.snackbar.SnackbarController
import ru.herobrine1st.e621.ui.snackbar.SnackbarMessage
import ru.herobrine1st.e621.ui.theme.E621Theme
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var snackbarMessagesFlow: MutableSharedFlow<SnackbarMessage>

    @Inject
    lateinit var snackbarAdapter: SnackbarAdapter

    @Inject
    lateinit var exoPlayer: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                applicationContext.getPreferencesFlow().first()
            } catch (t: Throwable) {
                Log.e(TAG, "An error occurred while pre-reading preferences", t)
            }
        }

        setContent {
            E621Theme(window) {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                // Navigation
                val navController = rememberNavController()


                // State

                val preferences by context.getPreferencesAsState()

                var showBlacklistDialog by remember { mutableStateOf(false) }
                val scaffoldState = rememberScaffoldState()
                SnackbarController(
                    snackbarMessagesFlow,
                    scaffoldState.snackbarHostState
                )
                CompositionLocalProvider(
                    LocalSnackbar provides snackbarAdapter,
                    LocalExoPlayer provides exoPlayer
                ) {
                    MainScaffold(
                        navController = navController,
                        scaffoldState = scaffoldState,
                        onOpenBlacklistDialog = { showBlacklistDialog = true })
                }

                if (showBlacklistDialog)
                    BlacklistTogglesDialog(
                        isBlacklistEnabled = preferences.blacklistEnabled,
                        toggleBlacklist = { enabled: Boolean ->
                            coroutineScope.launch {
                                context.updatePreferences { setBlacklistEnabled(enabled) }
                            }
                        },
                        onClose = { showBlacklistDialog = false })
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
    }
}