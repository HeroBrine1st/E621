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
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.ApiException
import ru.herobrine1st.e621.api.search.FavouritesSearchOptions
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.preference.proto.AuthorizationCredentialsOuterClass.AuthorizationCredentials
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.e621.util.InstanceBase
import ru.herobrine1st.e621.util.credentials
import java.io.IOException


interface IHomeComponent {
    val state: HomeComponent.LoginState

    fun login(login: String, apiKey: String, callback: (HomeComponent.LoginState) -> Unit = {})
    fun logout(callback: () -> Unit = {})
    fun retryStoredAuth()
    fun navigateToSearch()
    fun navigateToFavourites()
}

class HomeComponent(
    authorizationRepositoryProvider: Lazy<AuthorizationRepository>,
    apiProvider: Lazy<API>,
    private val snackbarAdapter: SnackbarAdapter,
    blacklistRepository: BlacklistRepository,
    private val stackNavigator: StackNavigator<Config>,
    private val exceptionReporter: ExceptionReporter,
    componentContext: ComponentContext,
) : ComponentContext by componentContext, IHomeComponent {

    private val instance = instanceKeeper.getOrCreate {
        HomeComponentInstance(
            authorizationRepositoryProvider,
            apiProvider,
            blacklistRepository
        )
    }

    private val lifecycleScope = LifecycleScope()

    override val state: LoginState
        get() = instance.state

    override fun login(login: String, apiKey: String, callback: (LoginState) -> Unit) {
        if (!state.canAuthorize) {
            Log.wtf(TAG, "Login attempted while impossible")
            return
        }
        lifecycleScope.launch {
            val state = instance.login(
                AuthorizationCredentials.newBuilder()
                    .setUsername(login)
                    .setPassword(apiKey)
                    .build()
            )
            if (state is LoginState.Authorized) {
                try {
                    // TODO preference, dialog, idk
                    instance.fetchBlacklistFromAccount(state)
                } catch (e: SQLiteException) {
                    snackbarAdapter.enqueueMessage(
                        R.string.database_error_updating_blacklist,
                        SnackbarDuration.Long
                    )
                } catch (t: Throwable) {
                    // TODO clarify
                    exceptionReporter.handleRequestException(t, showThrowable = true)
                }
            } else handleErrorStateForUI(state)
            callback(state)
        }
    }


    override fun logout(callback: () -> Unit) {
        lifecycleScope.launch {
            instance.logout()
            callback()
        }
    }

    override fun retryStoredAuth() {
        lifecycleScope.launch {
            val state = instance.tryStoredAuth()
            if (state != null) {
                handleErrorStateForUI(state)
            }
        }
    }

    override fun navigateToSearch() = stackNavigator.pushIndexed { Config.Search(index = it) }
    override fun navigateToFavourites() = stackNavigator.pushIndexed { index ->
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

        LoginState.UnknownError -> snackbarAdapter.enqueueMessage(R.string.unknown_error, SnackbarDuration.Long)

        LoginState.Loading -> throw IllegalStateException()

    }

    companion object {
        const val TAG = "HomeComponent"
    }

    class HomeComponentInstance(
        authorizationRepositoryProvider: Lazy<AuthorizationRepository>,
        apiProvider: Lazy<API>,
        private val blacklistRepository: BlacklistRepository,
    ) : InstanceBase() {

        var state by mutableStateOf<LoginState>(LoginState.Loading)
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

        suspend fun login(credentials: AuthorizationCredentials): LoginState {
            if (!state.canAuthorize) throw IllegalStateException()
            val result = tryCredentials(credentials)

            if (result is LoginState.Authorized) {
                authorizationRepository.insertAccount(credentials.username, credentials.password)
                state = result
            }

            return result
        }

        suspend fun logout() = authorizationRepository.logout()

        /**
         * Fetch authorization credentials from local store and try them,
         * returning [LoginState] or null if no credentials found.
         *
         * @return null if no credentials, otherwise [LoginState] indicating success or failure
         */
        suspend fun tryStoredAuth(): LoginState? {
            state = LoginState.Loading
            val entry = withContext(Dispatchers.Default) {
                authorizationRepository
            }.getAccountFlow()
                .distinctUntilChanged()
                .first()
            return if (entry == null) null
            else tryCredentials(entry).also {
                if (it == LoginState.NoAuth) {
                    authorizationRepository.logout()
                }
                assert(it != LoginState.Loading)
                state = it
            }
        }

        suspend fun fetchBlacklistFromAccount(state: LoginState.Authorized) {
            if (blacklistRepository.count() != 0) return

            api.getUser(name = state.username)
                .mapCatching { response ->
                    response["blacklisted_tags"]!!.jsonPrimitive
                        .content.split("\n")
                        .map { BlacklistEntry(query = it, enabled = true) }
                }.onFailure {
                    Log.e(TAG, "An error occurred while trying to fetch blacklist", it)
                    throw it
                }.mapCatching {
                    blacklistRepository.insertEntries(it)
                }.onFailure {
                    Log.e(TAG, "Database error while trying to insert fetched blacklist", it)
                }.getOrThrow()
        }

        private suspend fun tryCredentials(
            credentials: AuthorizationCredentials, // too connected to data store, should probably use own type
        ): LoginState {
            return api.getUser(credentials.username, credentials.credentials)
                .map { body ->
                    LoginState.Authorized(
                        body["name"]!!.jsonPrimitive.content,
                        body["id"]!!.jsonPrimitive.content.toInt()
                    )
                }
                .recover {
                    when (it) {
                        is ApiException -> {
                            val status = it.status
                            when {
                                status == HttpStatusCode.Unauthorized -> LoginState.NoAuth
                                status == HttpStatusCode.ServiceUnavailable -> LoginState.APITemporarilyUnavailable // Likely DDoS protection, but not always
                                status.value in 500..599 -> LoginState.InternalServerError
                                else -> {
                                    Log.w(
                                        TAG,
                                        "Unknown API error occurred while authenticating: $status"
                                    )
                                    LoginState.UnknownAPIError
                                }
                            }
                        }

                        is IOException -> {
                            Log.e(
                                TAG,
                                "A network exception occurred while checking authorization data",
                                it
                            )
                            LoginState.IOError
                        }

                        else -> {
                            Log.e(
                                TAG,
                                "Unknown error occurred while checking authorization data",
                                it
                            )
                            LoginState.UnknownError
                        }
                    }
                }.getOrThrow()
        }
    }

    // Can authorize is "can press login button"
    sealed class LoginState(val canAuthorize: Boolean) {
        data object Loading : LoginState(false)
        data object IOError : LoginState(false)
        data object InternalServerError : LoginState(false)
        data object UnknownAPIError : LoginState(false)
        data object APITemporarilyUnavailable : LoginState(false)
        data object NoAuth : LoginState(true)
        class Authorized(val username: String, val id: Int) : LoginState(false)

        data object UnknownError : LoginState(false)
    }


}