package ru.herobrine1st.e621.ui.screen.home

import android.util.Log
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

    fun login(login: String, apiKey: String, callback: (LoginState) -> Unit = {}) {
        if (!state.canAuthorize) throw IllegalStateException()
        viewModelScope.launch {
            val state = checkCredentials(
                AuthorizationCredentials.newBuilder()
                    .setUsername(login)
                    .setPassword(apiKey)
                    .build()
            )
            if (state == LoginState.AUTHORIZED)
                authorizationRepositoryProvider.get().insertAccount(login, apiKey)
            callback(state)
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
                apiProvider.get().getUser(credentials.username, credentials.credentials).awaitResponse()
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
        return if (res.isSuccessful) {
            LoginState.AUTHORIZED
        } else {
            debug {
                Log.d(TAG, "Invalid username or api key (${res.code()} ${res.message()})")
                withContext(Dispatchers.IO) {
                    @Suppress("BlockingMethodInNonBlockingContext") // Debug
                    Log.d(TAG, res.errorBody()!!.string())
                }
            }
            LoginState.NO_AUTH
        }
    }

    private suspend fun checkAuthorizationInternal() {
        val entry = withContext(Dispatchers.Default) {
            authorizationRepositoryProvider.get()
        }.getAccountFlow()
            .distinctUntilChanged()
            .first()
        state = (if (entry == null) LoginState.NO_AUTH
        else checkCredentials(entry).also {
            if (it == LoginState.NO_AUTH) {
                snackbarAdapter.enqueueMessage(R.string.login_unauthorized)
                authorizationRepositoryProvider.get().logout()
            }
        })
    }

    // Can authorize is "can press login button"
    enum class LoginState(val canAuthorize: Boolean) {
        LOADING(false),
        IO_ERROR(false),
        NO_AUTH(true),
        AUTHORIZED(false)
    }

    companion object {
        const val TAG = "HomeViewModel"
    }
}