/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
import ru.herobrine1st.e621.preference.*
import ru.herobrine1st.e621.ui.MainScaffold
import ru.herobrine1st.e621.ui.component.legal.LicenseAndDisclaimerInitialDialogs
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
                val preferences by context.dataStore.data.collectAsState(initial = PreferencesSerializer.defaultValue)

                var showBlacklistDialog by remember { mutableStateOf(false) }
                val scaffoldState = rememberScaffoldState()
                SnackbarController(
                    snackbarMessagesFlow,
                    scaffoldState.snackbarHostState
                )
                CompositionLocalProvider(
                    LocalSnackbar provides snackbarAdapter,
                    LocalExoPlayer provides exoPlayer,
                    LocalPreferences provides preferences
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
                LicenseAndDisclaimerInitialDialogs(hasShownBefore = preferences.licenseAndNonAffiliationDisclaimerShown) {
                    coroutineScope.launch {
                        context.updatePreferences {
                            licenseAndNonAffiliationDisclaimerShown = true
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
    }
}