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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.google.android.exoplayer2.ExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.navigation.component.root.RootComponent
import ru.herobrine1st.e621.navigation.component.root.RootComponentImpl
import ru.herobrine1st.e621.preference.*
import ru.herobrine1st.e621.ui.Navigator
import ru.herobrine1st.e621.ui.component.legal.LicenseAndDisclaimerInitialDialogs
import ru.herobrine1st.e621.ui.dialog.BlacklistTogglesDialog
import ru.herobrine1st.e621.ui.snackbar.LocalSnackbar
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.ui.snackbar.SnackbarController
import ru.herobrine1st.e621.ui.snackbar.SnackbarMessage
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor
import ru.herobrine1st.e621.ui.theme.E621Theme
import ru.herobrine1st.e621.util.*
import java.net.Authenticator
import java.net.Proxy
import java.net.ProxySelector
import javax.inject.Inject
import javax.inject.Provider


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var snackbarMessagesFlow: MutableSharedFlow<SnackbarMessage>

    @Inject
    lateinit var snackbarAdapter: SnackbarAdapter

    @Inject
    lateinit var apiProvider: Provider<API>

    @Inject
    lateinit var authorizationRepositoryProvider: Provider<AuthorizationRepository>

    @Inject
    lateinit var blacklistRepositoryProvider: Provider<BlacklistRepository>

    @Inject
    lateinit var favouritesCacheProvider: Provider<FavouritesCache>

    @Inject
    lateinit var exceptionReporterProvider: Provider<ExceptionReporter>

    @Inject
    lateinit var exoPlayerProvider: Provider<ExoPlayer>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up proxy
        // Also pre-read preferences early
        getPreferencesFlow()
            .onEach { preferences ->
                val proxies = if (preferences.hasProxy() && preferences.proxy.enabled)
                    listOf(ProxyWithAuth(preferences.proxy)) else emptyList()
                Authenticator.setDefault(AuthenticatorImpl(proxies))
                // TODO add fall back preference (maybe after multiple proxies support)
                ProxySelector.setDefault(ProxySelectorImpl(proxies + Proxy.NO_PROXY))
            }.catch { t ->
                Log.wtf(TAG, "An error occurred while setting up proxy", t)
            }
            .take(1) // Looks like restart is required. I think OkHttp somehow handles proxy by itself.
            .launchIn(lifecycleScope)

        val rootComponent = RootComponentImpl(
            applicationContext,
            authorizationRepositoryProvider = authorizationRepositoryProvider.lazy(),
            apiProvider = apiProvider.lazy(),
            snackbarAdapterProvider = lazyOf(snackbarAdapter),
            blacklistRepositoryProvider = blacklistRepositoryProvider.lazy(),
            favouritesCacheProvider = favouritesCacheProvider.lazy(),
            exceptionReporterProvider = exceptionReporterProvider.lazy(),
            exoPlayerProvider = exoPlayerProvider.lazy(),
            componentContext = defaultComponentContext()
        )

        setContent {
            E621Theme(window) {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                // State
                val preferences by context.dataStore.data.collectAsState(initial = PreferencesSerializer.defaultValue)

                val snackbarHostState = remember { SnackbarHostState() }
                SnackbarController(
                    snackbarMessagesFlow,
                    snackbarHostState
                )

                CompositionLocalProvider(
                    LocalSnackbar provides snackbarAdapter,
                    LocalPreferences provides preferences
                ) {
                    Box(Modifier.background(MaterialTheme.colors.background)) {
                        // TODO replace with StackAnimation which is aware of previous and current component
                        //      so it can place corresponding appbars
                        TopAppBar(
                            title = {},
                            actions = {
                                IconButton(onClick = {}) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = null,
                                        tint = ActionBarIconColor
                                    )
                                }
                            },
                            backgroundColor = MaterialTheme.colors.primarySurface,
                            modifier = Modifier.align(Alignment.TopStart)
                        )

                        Navigator(rootComponent, snackbarHostState)

                        // weak TO/DO: move SnackbarHost here, as animation should not include SnackbarHost
                        // scaffold places snackbar according to FAB and BottomBar,
                        // so it is not possible to calculate it from here
                    }

                    val dialog by rootComponent.dialogSlot.subscribeAsState()
                    when (val instance = dialog.child?.instance) {
                        is RootComponent.DialogChild.BlacklistToggles -> {
                            BlacklistTogglesDialog(component = instance.component)
                        }
                        null -> {}
                    }

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
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
    }
}