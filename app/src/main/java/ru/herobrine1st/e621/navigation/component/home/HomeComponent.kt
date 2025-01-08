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

package ru.herobrine1st.e621.navigation.component.home

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.essenty.lifecycle.doOnResume
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.ApiException
import ru.herobrine1st.e621.api.search.FavouritesSearchOptions
import ru.herobrine1st.e621.database.entity.BlacklistEntry
import ru.herobrine1st.e621.database.repository.authorization.AuthorizationRepository
import ru.herobrine1st.e621.database.repository.blacklist.BlacklistRepository
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.navigation.component.home.IHomeComponent.LoginState
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.ExceptionReporter
import java.io.IOException

private const val TAG = "HomeComponent"

interface IHomeComponent {
    val state: LoginState

    fun login(username: String, apiKey: String, callback: (isSuccess: Boolean) -> Unit = {})
    fun logout(callback: () -> Unit = {})
    fun navigateToSearch()
    fun navigateToFavourites()

    // Can authorize is "can press login button"
    sealed class LoginState(val canAuthorize: Boolean) {
        data object Loading : LoginState(false)
        data object NoAuth : LoginState(true)
        class Authorized(val username: String, val id: Int) : LoginState(false)
    }
}

class HomeComponent(
    authorizationRepositoryProvider: Lazy<AuthorizationRepository>,
    apiProvider: Lazy<API>,
    private val snackbarAdapter: SnackbarAdapter,
    private val blacklistRepository: BlacklistRepository,
    private val stackNavigator: StackNavigator<Config>,
    private val exceptionReporter: ExceptionReporter,
    componentContext: ComponentContext,
) : ComponentContext by componentContext, IHomeComponent {

    private val authorizationRepository by authorizationRepositoryProvider
    private val api by apiProvider
    private val lifecycleScope = LifecycleScope()

    override var state by mutableStateOf<LoginState>(LoginState.Loading)

    init {
        lifecycle.doOnResume {
            lifecycleScope.launch {
                authorizationRepository.getAccount()?.let { entry ->
                    LoginState.Authorized(entry.username, entry.id)
                        .also { state = it }
                }
                authorizationRepository.getAccountFlow().collect {
                    if (it == null) // Handle logout from AuthorizationInterceptor
                        state = LoginState.NoAuth
                }
            }
        }
    }

    override fun login(username: String, apiKey: String, callback: (isSuccess: Boolean) -> Unit) {
        if (!state.canAuthorize) {
            Log.wtf(TAG, "Login attempted while impossible")
            return
        }
        lifecycleScope.launch {
            val state = testCredentials(username, apiKey)
            if (state is CredentialsTestState.Authorized) {
                this@HomeComponent.state =
                    LoginState.Authorized(username = state.username, id = state.id)
                try {
                    // TODO preference, dialog, idk
                    fetchBlacklistFromAccount(state)
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
            callback(state is CredentialsTestState.Authorized)
        }
    }

    override fun logout(callback: () -> Unit) {
        lifecycleScope.launch {
            authorizationRepository.logout()
            callback()
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

    private fun handleErrorStateForUI(state: CredentialsTestState) = lifecycleScope.launch {
        when (state) {
            is CredentialsTestState.Authorized -> {} // Success

            CredentialsTestState.IOError -> snackbarAdapter.enqueueMessage(
                R.string.network_error,
                SnackbarDuration.Long
            )

            CredentialsTestState.NoAuth -> snackbarAdapter.enqueueMessage(
                R.string.login_unauthorized,
                SnackbarDuration.Long
            )

            CredentialsTestState.InternalServerError -> snackbarAdapter.enqueueMessage(
                R.string.internal_server_error, SnackbarDuration.Long
            )

            CredentialsTestState.APITemporarilyUnavailable -> snackbarAdapter.enqueueMessage(
                R.string.api_temporarily_unavailable, SnackbarDuration.Long
            )

            CredentialsTestState.UnknownAPIError -> snackbarAdapter.enqueueMessage(
                R.string.unknown_api_error, SnackbarDuration.Long
            )

            CredentialsTestState.UnknownError -> snackbarAdapter.enqueueMessage(
                R.string.unknown_error,
                SnackbarDuration.Long
            )
        }
    }

    private suspend fun fetchBlacklistFromAccount(state: CredentialsTestState.Authorized) {
        if (blacklistRepository.count() != 0) return

        api.getUser(name = state.username)
            .mapCatching { response ->
                response["blacklisted_tags"]!!.jsonPrimitive
                    .content.split("\n")
                    .map { BlacklistEntry(query = it, enabled = true) }
            }.onFailure {
                Log.e(TAG, "An error occurred while trying to fetch blacklist", it)
                lifecycleScope.launch {
                    snackbarAdapter.enqueueMessage(R.string.blacklist_fetch_error)
                }
            }.mapCatching {
                blacklistRepository.insertEntries(it)
            }.onFailure {
                Log.e(TAG, "Database error occurred while trying to insert fetched blacklist", it)
            }.getOrThrow()
    }

    private suspend fun testCredentials(
        username: String, apiKey: String
    ): CredentialsTestState = api.authenticate(username, apiKey)
        .onSuccess { authorizationRepository.insertAccount(it) }
        .map {
            CredentialsTestState.Authorized(
                username = it.username,
                id = it.id
            )
        }
        .getOrElse {
            when (it) {
                is ApiException -> when {
                    it.status == HttpStatusCode.Unauthorized -> CredentialsTestState.NoAuth
                    it.status == HttpStatusCode.ServiceUnavailable -> CredentialsTestState.APITemporarilyUnavailable
                    it.status.value in 500..599 -> CredentialsTestState.InternalServerError
                    else -> CredentialsTestState.UnknownAPIError
                }

                is IOException -> CredentialsTestState.IOError
                else -> CredentialsTestState.UnknownError
            }
        }

    sealed interface CredentialsTestState {
        data object IOError : CredentialsTestState
        data object InternalServerError : CredentialsTestState
        data object UnknownAPIError : CredentialsTestState
        data object APITemporarilyUnavailable : CredentialsTestState
        data object NoAuth : CredentialsTestState
        class Authorized(val username: String, val id: Int) : CredentialsTestState
        data object UnknownError : CredentialsTestState
    }
}