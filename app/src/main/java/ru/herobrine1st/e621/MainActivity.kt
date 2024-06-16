/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.module.ActivityInjectionCompanion
import ru.herobrine1st.e621.module.CachedDataStore
import ru.herobrine1st.e621.navigation.component.root.RootComponent
import ru.herobrine1st.e621.navigation.component.root.RootComponentImpl
import ru.herobrine1st.e621.preference.PreferencesSerializer
import ru.herobrine1st.e621.preference.updatePreferences
import ru.herobrine1st.e621.ui.Navigator
import ru.herobrine1st.e621.ui.component.legal.LicenseAndDisclaimerInitialDialogs
import ru.herobrine1st.e621.ui.dialog.BlacklistTogglesDialog
import ru.herobrine1st.e621.ui.theme.E621Theme
import ru.herobrine1st.e621.ui.theme.snackbar.LocalSnackbar
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarController
import ru.herobrine1st.e621.util.AuthenticatorImpl
import ru.herobrine1st.e621.util.ProxySelectorImpl
import ru.herobrine1st.e621.util.ProxyWithAuth
import java.net.Authenticator
import java.net.Proxy
import java.net.ProxySelector


class MainActivity : ComponentActivity() {
    class ViewModelForRetaining(application: android.app.Application) : AndroidViewModel(application) {

        val injectionCompanion = ActivityInjectionCompanion(
            applicationInjectionCompanion = getApplication<Application>().injectionCompanion
        )

        override fun onCleared() {
            injectionCompanion.onDestroy()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val viewModel: ViewModelForRetaining by viewModels()
        val injectionCompanion = viewModel.injectionCompanion

        // Set up proxy
        // Also pre-read preferences early
        injectionCompanion.dataStoreModule.dataStore.data
            .onEach { preferences ->
                val proxies = if (preferences.proxy != null && preferences.proxy.enabled)
                    listOf(ProxyWithAuth(preferences.proxy)) else emptyList()
                Authenticator.setDefault(AuthenticatorImpl(proxies))
                // TODO add fall back preference (maybe after multiple proxies support)
                ProxySelector.setDefault(ProxySelectorImpl(proxies + Proxy.NO_PROXY))
            }.catch { t ->
                Log.wtf(TAG, "An error occurred while setting up proxy", t)
            }
            .take(1) // Looks like restart is required. I think OkHttp somehow handles proxy by itself.
            .launchIn(lifecycleScope)

        @OptIn(CachedDataStore::class) // And also start StateFlow by initializing Lazy
        injectionCompanion.dataStoreModule.cachedData

        val rootComponent = RootComponentImpl(
            injectionCompanion = injectionCompanion,
            componentContext = defaultComponentContext()
        )

        setContent {
            E621Theme {
                val coroutineScope = rememberCoroutineScope()

                val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
                SnackbarController(
                    injectionCompanion.snackbarModule.snackbarMessageFlow,
                    snackbarHostState
                )

                CompositionLocalProvider(
                    LocalSnackbar provides injectionCompanion.snackbarModule.snackbarAdapter
                ) {
                    Box(Modifier.background(MaterialTheme.colorScheme.background)) {
                        Navigator(rootComponent, snackbarHostState)

                        // weak TO/DO: move SnackbarHost here, as animation should not include SnackbarHost
                        // scaffold places snackbar according to FAB and BottomBar,
                        // so it is not possible to calculate it from here

                        // or simply separate snackbar states. It is simple and easy.
                    }

                    val dialog by rootComponent.dialogSlot.subscribeAsState()
                    when (val instance = dialog.child?.instance) {
                        is RootComponent.DialogChild.BlacklistToggles -> {
                            BlacklistTogglesDialog(component = instance.component)
                        }
                        null -> {}
                    }

                    val preferences by rootComponent.dataStore.data.collectAsState(initial = PreferencesSerializer.defaultValue)
                    LicenseAndDisclaimerInitialDialogs(hasShownBefore = preferences.licenseAndNonAffiliationDisclaimerShown) {
                        coroutineScope.launch {
                            rootComponent.dataStore.updatePreferences {
                                copy(licenseAndNonAffiliationDisclaimerShown = true)
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