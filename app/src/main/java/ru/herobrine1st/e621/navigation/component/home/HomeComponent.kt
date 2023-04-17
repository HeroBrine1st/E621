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

package ru.herobrine1st.e621.navigation.component.home

import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.FavouritesSearchOptions
import ru.herobrine1st.e621.api.awaitResponse
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.preference.proto.AuthorizationCredentialsOuterClass.AuthorizationCredentials
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.InstanceBase
import ru.herobrine1st.e621.util.credentials
import ru.herobrine1st.e621.util.debug
import ru.herobrine1st.e621.util.pushIndexed
import java.io.IOException


interface IHomeComponentInstanceMethods {
    val state: HomeComponent.LoginState

    fun login(login: String, apiKey: String, callback: (HomeComponent.LoginState) -> Unit = {})
    fun logout()
    fun checkAuthorization()
}

class HomeComponent private constructor(
    private val stackNavigator: StackNavigator<Config>,
    val instance: HomeComponentInstance,
    componentContext: ComponentContext
) : ComponentContext by componentContext, IHomeComponentInstanceMethods by instance {

    fun navigateToSearch() = stackNavigator.pushIndexed { Config.Search(index = it) }
    fun navigateToFavourites() = stackNavigator.pushIndexed { index ->
        (state as? LoginState.Authorized)?.let {
            Config.PostListing(
                FavouritesSearchOptions(
                    // null user id = self
                    favouritesOf = it.username,
                    id = it.id
                ),
                index = index
            )
        } ?: error("Inconsistent state: state is not Authorized while is inferred to be so")
    }


    companion object {
        const val TAG = "HomeViewModel"

        operator fun invoke(
            authorizationRepositoryProvider: Lazy<AuthorizationRepository>,
            apiProvider: Lazy<API>,
            snackbarAdapter: SnackbarAdapter,
            stackNavigator: StackNavigator<Config>,
            componentContext: ComponentContext
        ): HomeComponent {
            val instance = componentContext.instanceKeeper.getOrCreate {
                HomeComponentInstance(
                    authorizationRepositoryProvider, apiProvider, snackbarAdapter
                )
            }
            return HomeComponent(stackNavigator, instance, componentContext)
        }
    }

    class HomeComponentInstance(
        authorizationRepositoryProvider: Lazy<AuthorizationRepository>,
        apiProvider: Lazy<API>,
        private val snackbarAdapter: SnackbarAdapter,
    ) : InstanceBase(), IHomeComponentInstanceMethods {

        override var state by mutableStateOf<LoginState>(LoginState.Loading)
            private set
        var username: String? = null
        var id: Int? = null

        private val authorizationRepository by authorizationRepositoryProvider
        private val api by apiProvider

        // Sources of authorization:
        // This VM: login/logout
        // AuthorizationInterceptor: logout only
        // So we first check auth at the start of this VM and then listen to logouts
        init {
            lifecycleScope.launch {
                checkAuthorizationInternal()
                authorizationRepository.getAccountFlow().collect {
                    if (it == null) // Handle logout from AuthorizationInterceptor
                        state = LoginState.NoAuth
                }
            }
        }

        // Should not be used in this VM
        override fun login(login: String, apiKey: String, callback: (LoginState) -> Unit) {
            if (!state.canAuthorize) throw IllegalStateException()
            lifecycleScope.launch {
                val result = checkCredentials(
                    AuthorizationCredentials.newBuilder()
                        .setUsername(login)
                        .setPassword(apiKey)
                        .build()
                )
                callback(result)
                when (result) {
                    is LoginState.Authorized -> {
                        authorizationRepository.insertAccount(login, apiKey)
                        state = result
                    }
                    LoginState.IOError ->
                        snackbarAdapter.enqueueMessage(
                            R.string.network_error,
                            SnackbarDuration.Long
                        )
                    LoginState.NoAuth -> snackbarAdapter.enqueueMessage(
                        R.string.login_unauthorized,
                        SnackbarDuration.Long
                    )
                    LoginState.InternalServerError -> snackbarAdapter.enqueueMessage(
                        R.string.internal_server_error, SnackbarDuration.Long
                    )
                    LoginState.APITemporarilyUnavailable -> snackbarAdapter.enqueueMessage(
                        R.string.api_temporarily_unavailable, SnackbarDuration.Long
                    )
                    LoginState.UnknownAPIError -> snackbarAdapter.enqueueMessage(
                        R.string.unknown_api_error, SnackbarDuration.Long
                    )
                    LoginState.Loading -> throw IllegalStateException()
                }
            }
        }

        override fun logout() {
            lifecycleScope.launch {
                authorizationRepository.logout()
            }
        }

        override fun checkAuthorization() {
            lifecycleScope.launch {
                checkAuthorizationInternal()
            }
        }

        private suspend fun checkCredentials(credentials: AuthorizationCredentials): LoginState {
            val res = try {
                withContext(Dispatchers.IO) {
                    api.getUser(credentials.username, credentials.credentials)
                        .awaitResponse()
                }
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "A network exception occurred while checking authorization data",
                    e
                )
                snackbarAdapter.enqueueMessage(R.string.network_error)
                return LoginState.IOError
            }
            debug {
                if (!res.isSuccessful) {
                    Log.d(
                        TAG,
                        "An error occurred while authenticating in API (${res.code()} ${res.message()})"
                    )
                    withContext(Dispatchers.IO) {
                        @Suppress("BlockingMethodInNonBlockingContext") // Debug
                        Log.d(TAG, res.errorBody()!!.string())
                    }
                }
            }
            return when (res.code()) {
                in 200..299 -> {
                    val body = res.body()!!
                    LoginState.Authorized(body.get("name").asText(), body.get("id").asInt())
                }
                401 -> LoginState.NoAuth
                503 -> LoginState.APITemporarilyUnavailable // Likely DDoS protection, but not always
                in 500..599 -> LoginState.InternalServerError
                else -> {
                    Log.w(
                        TAG,
                        "Unknown API error occurred while authenticating (${res.code()} ${res.message()})"
                    )
                    LoginState.UnknownAPIError
                }
            }
        }


        private suspend fun checkAuthorizationInternal() {
            state = LoginState.Loading
            val entry = withContext(Dispatchers.Default) {
                authorizationRepository
            }.getAccountFlow()
                .distinctUntilChanged()
                .first()
            state = if (entry == null) LoginState.NoAuth else checkCredentials(entry).also {
                if (it == LoginState.NoAuth) {
                    snackbarAdapter.enqueueMessage(R.string.login_unauthorized)
                    authorizationRepository.logout()
                }
            }
            assert(state != LoginState.Loading)
        }
    }

    // Can authorize is "can press login button"
    sealed class LoginState(val canAuthorize: Boolean) {
        object Loading : LoginState(false)
        object IOError : LoginState(false)
        object InternalServerError : LoginState(false)
        object UnknownAPIError : LoginState(false)
        object APITemporarilyUnavailable : LoginState(false)
        object NoAuth : LoginState(true)
        class Authorized(val username: String, val id: Int) : LoginState(false)
    }


}