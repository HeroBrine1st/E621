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

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.FavouritesSearchOptions
import ru.herobrine1st.e621.api.await
import ru.herobrine1st.e621.api.awaitResponse
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.preference.proto.AuthorizationCredentialsOuterClass.AuthorizationCredentials
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.InstanceBase
import ru.herobrine1st.e621.util.credentials
import ru.herobrine1st.e621.util.debug
import java.io.IOException


interface IHomeComponentInstanceMethods {
    val state: HomeComponent.LoginState

    fun login(login: String, apiKey: String, callback: (HomeComponent.LoginState) -> Unit = {})
    fun logout(callback: () -> Unit = {})
    fun retryStoredAuth()
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
                    favouritesOf = it.username,
                    id = it.id
                ),
                index = index
            )
        } ?: error("Inconsistent state: state is not Authorized while is inferred to be so")
    }

    companion object {
        const val TAG = "HomeComponent"

        operator fun invoke(
            authorizationRepositoryProvider: Lazy<AuthorizationRepository>,
            apiProvider: Lazy<API>,
            snackbarAdapter: SnackbarAdapter,
            blacklistRepository: BlacklistRepository,
            stackNavigator: StackNavigator<Config>,
            componentContext: ComponentContext
        ): HomeComponent {
            val instance = componentContext.instanceKeeper.getOrCreate {
                HomeComponentInstance(
                    authorizationRepositoryProvider,
                    apiProvider,
                    snackbarAdapter,
                    blacklistRepository
                )
            }
            return HomeComponent(stackNavigator, instance, componentContext)
        }
    }

    class HomeComponentInstance(
        authorizationRepositoryProvider: Lazy<AuthorizationRepository>,
        apiProvider: Lazy<API>,
        private val snackbarAdapter: SnackbarAdapter,
        private val blacklistRepository: BlacklistRepository
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
                tryStoredAuth()
                authorizationRepository.getAccountFlow().collect {
                    if (it == null) // Handle logout from AuthorizationInterceptor
                        state = LoginState.NoAuth
                }
            }
        }

        //region Interface implementation

        override fun login(login: String, apiKey: String, callback: (LoginState) -> Unit) {
            if (!state.canAuthorize) throw IllegalStateException()
            lifecycleScope.launch {
                val result = tryCredentials(
                    AuthorizationCredentials.newBuilder()
                        .setUsername(login)
                        .setPassword(apiKey)
                        .build()
                )

                if (result is LoginState.Authorized) {
                    authorizationRepository.insertAccount(login, apiKey)
                    fetchBlacklistFromAccount(result)
                    state = result
                } else handleErrorStateForUI(result)
                callback(result)
            }
        }

        override fun logout(callback: () -> Unit) {
            lifecycleScope.launch {
                authorizationRepository.logout()
                callback()
            }
        }


        override fun retryStoredAuth() {
            lifecycleScope.launch {
                tryStoredAuth()
            }
        }

        //endregion

        // I think this code should be moved to component
        // and component should act as proxy between logic (this class) and UI
        private suspend fun handleErrorStateForUI(state: LoginState) = when (state) {
            is LoginState.Authorized -> {} // Success

            LoginState.IOError -> snackbarAdapter.enqueueMessage(
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


        private suspend fun tryCredentials(
            credentials: AuthorizationCredentials // too connected to data store, should probably use own type
        ): LoginState {
            val res = try {
                api.getUser(credentials.username, credentials.credentials)
                    .awaitResponse()

            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "A network exception occurred while checking authorization data",
                    e
                )
                return LoginState.IOError
            }
            debug {
                if (!res.isSuccessful) {
                    Log.d(
                        TAG,
                        "An error occurred while authenticating in API (${res.code()} ${res.message()})"
                    )
                    withContext(Dispatchers.IO) {
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


        private suspend fun tryStoredAuth() {
            state = LoginState.Loading
            val entry = withContext(Dispatchers.Default) {
                authorizationRepository
            }.getAccountFlow()
                .distinctUntilChanged()
                .first()
            state = if (entry == null) LoginState.NoAuth else tryCredentials(entry).also {
                handleErrorStateForUI(it)
                if (it == LoginState.NoAuth) {
                    authorizationRepository.logout()
                }
            }
            assert(state != LoginState.Loading)
        }

        private suspend fun fetchBlacklistFromAccount(state: LoginState.Authorized) {
            if (blacklistRepository.count() != 0) return
            val entries =
                api.getUser(name = state.username).await()
                    .get("blacklisted_tags").asText()
                    .split("\n")
                    .map {
                        BlacklistEntry(query = it, enabled = true)
                    }
            try {
                blacklistRepository.insertEntries(entries)
            } catch (e: SQLiteException) {
                Log.e(TAG, "SQLite Error while trying to add tag to blacklist", e)
                snackbarAdapter.enqueueMessage(
                    R.string.database_error_updating_blacklist,
                    SnackbarDuration.Long
                )
                return
            }
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