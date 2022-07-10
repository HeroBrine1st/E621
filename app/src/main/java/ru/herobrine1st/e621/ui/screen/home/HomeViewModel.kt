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

    init {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                authorizationRepositoryProvider.get()
            }.getAccountFlow()
                .distinctUntilChanged()
                .collect { entry ->
                    state = LoginState.LOADING
                    checkAuthorization(entry)
                }
        }
    }

    fun login(login: String, apiKey: String, callback: () -> Unit = {}) {
        if (!state.canAuthorize) throw IllegalStateException()
        viewModelScope.launch {
            authorizationRepositoryProvider.get().insertAccount(login, apiKey)
            callback()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authorizationRepositoryProvider.get().logout()
        }
    }

    private suspend fun checkAuthorization(auth: AuthorizationCredentials?) {
        if (auth == null) {
            state = LoginState.NO_AUTH
            return
        }
        val res = try {
            withContext(Dispatchers.Default) {
                apiProvider.get()
            }.getUser(
                auth.username, auth.credentials
            ).awaitResponse()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "A network exception occurred while checking authorization data",
                e
            )
            snackbarAdapter.enqueueMessage(R.string.network_error)
            state = LoginState.IO_ERROR
            return
        }
        if (res.isSuccessful) {
            state = LoginState.AUTHORIZED
        } else {
            debug {
                Log.d(TAG, "Invalid username or api key (${res.code()} ${res.message()})")
                @Suppress("BlockingMethodInNonBlockingContext") // Debug
                Log.d(TAG, res.errorBody()!!.string())
            }
            snackbarAdapter.enqueueMessage(R.string.login_unauthorized)
            state = LoginState.NO_AUTH
            authorizationRepositoryProvider.get().logout()
        }
    }

    // Can authorize is "can press login button"
    // TODO rethink error handling logic (at now IO_ERROR doesn't mean there's no credentials, but user sees fields for credentials)
    enum class LoginState(val canAuthorize: Boolean) {
        LOADING(false),
        IO_ERROR(true),
        NO_AUTH(true),
        AUTHORIZED(false)
    }

    companion object {
        const val TAG = "HomeViewModel"
    }
}