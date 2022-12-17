/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.ui.screen.home

import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.preference.proto.AuthorizationCredentialsOuterClass.AuthorizationCredentials
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.credentials
import ru.herobrine1st.e621.util.debug
import java.io.IOException
import javax.inject.Inject
import javax.inject.Provider

// LoginScreenViewModel ?
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authorizationRepositoryProvider: Provider<AuthorizationRepository>,
    private val apiProvider: Provider<API>,
    private val snackbarAdapter: SnackbarAdapter
) : ViewModel() {
    var state by mutableStateOf(LoginState.LOADING)
        private set


    // Sources of authorization:
    // This VM: login/logout
    // AuthorizationInterceptor: logout only
    // So we first check auth at the start of this VM and then listen to logouts
    init {
        viewModelScope.launch {
            checkAuthorizationInternal()
            authorizationRepositoryProvider.get().getAccountFlow().collect {
                if (it == null) // Handle logout from AuthorizationInterceptor
                    state = LoginState.NO_AUTH
            }
        }
    }

    // Should not be used in this VM
    fun login(login: String, apiKey: String, callback: (LoginState) -> Unit = {}) {
        if (!state.canAuthorize) throw IllegalStateException()
        viewModelScope.launch {
            val result = checkCredentials(
                AuthorizationCredentials.newBuilder()
                    .setUsername(login)
                    .setPassword(apiKey)
                    .build()
            )
            callback(result)
            when (result) {
                LoginState.AUTHORIZED -> {
                    authorizationRepositoryProvider.get().insertAccount(login, apiKey)
                    state = LoginState.AUTHORIZED
                }
                LoginState.IO_ERROR ->
                    snackbarAdapter.enqueueMessage(R.string.network_error, SnackbarDuration.Long)
                LoginState.NO_AUTH -> snackbarAdapter.enqueueMessage(
                    R.string.login_unauthorized,
                    SnackbarDuration.Long
                )
                LoginState.INTERNAL_SERVER_ERROR -> snackbarAdapter.enqueueMessage(
                    R.string.internal_server_error, SnackbarDuration.Long
                )
                LoginState.API_TEMPORARILY_UNAVAILABLE -> snackbarAdapter.enqueueMessage(
                    R.string.api_temporarily_unavailable, SnackbarDuration.Long
                )
                LoginState.UNKNOWN_API_ERROR -> snackbarAdapter.enqueueMessage(
                    R.string.unknown_api_error, SnackbarDuration.Long
                )
                LoginState.LOADING -> throw IllegalStateException()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authorizationRepositoryProvider.get().logout()
        }
    }

    fun checkAuthorization() {
        viewModelScope.launch {
            checkAuthorizationInternal()
        }
    }

    private suspend fun checkCredentials(credentials: AuthorizationCredentials): LoginState {
        val res = try {
            withContext(Dispatchers.IO) {
                apiProvider.get().getUser(credentials.username, credentials.credentials)
                    .awaitResponse()
            }
        } catch (e: IOException) {
            Log.e(
                TAG,
                "A network exception occurred while checking authorization data",
                e
            )
            snackbarAdapter.enqueueMessage(R.string.network_error)
            return LoginState.IO_ERROR
        }
        debug {
            if(!res.isSuccessful) {
                Log.d(TAG, "An error occurred while authenticating in API (${res.code()} ${res.message()})")
                withContext(Dispatchers.IO) {
                    @Suppress("BlockingMethodInNonBlockingContext") // Debug
                    Log.d(TAG, res.errorBody()!!.string())
                }
            }
        }
        return when(res.code()) {
            in 200..299 -> LoginState.AUTHORIZED
            401 -> LoginState.NO_AUTH

            503 -> LoginState.API_TEMPORARILY_UNAVAILABLE // Likely DDoS protection, but not always
            in 500..599 -> LoginState.INTERNAL_SERVER_ERROR
            else -> LoginState.UNKNOWN_API_ERROR
        }
    }


    private suspend fun checkAuthorizationInternal() {
        state = LoginState.LOADING
        val entry = withContext(Dispatchers.Default) {
            authorizationRepositoryProvider.get()
        }.getAccountFlow()
            .distinctUntilChanged()
            .first()
        state = if (entry == null) LoginState.NO_AUTH else checkCredentials(entry).also {
            if (it == LoginState.NO_AUTH) {
                snackbarAdapter.enqueueMessage(R.string.login_unauthorized)
                authorizationRepositoryProvider.get().logout()
            }
        }
        assert(state != LoginState.LOADING)
    }

    // Can authorize is "can press login button"
    enum class LoginState(val canAuthorize: Boolean) {
        LOADING(false),
        IO_ERROR(false),
        INTERNAL_SERVER_ERROR(false),
        UNKNOWN_API_ERROR(true),
        API_TEMPORARILY_UNAVAILABLE(false),
        NO_AUTH(true),
        AUTHORIZED(false)
    }

    companion object {
        const val TAG = "HomeViewModel"
    }
}