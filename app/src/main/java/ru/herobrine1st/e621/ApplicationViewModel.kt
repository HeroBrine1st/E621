package ru.herobrine1st.e621

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.entity.Auth
import ru.herobrine1st.e621.entity.Blacklist
import java.io.IOException


enum class AuthState {
    NO_DATA, // default if no auth info at the start of app
    LOADING,
    AUTHORIZED,
    IO_ERROR,
    SQL_ERROR,
    UNAUTHORIZED // error when trying to authenticate
}

class ApplicationViewModel(val database: Database) : ViewModel() {
    class Factory(val database: Database) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ApplicationViewModel(database) as T
        }
    }

    companion object {
        val TAG = ApplicationViewModel::class.simpleName
    }

    init {
        loadAuthDataFromDatabase()
    }

    var authState: AuthState by mutableStateOf(AuthState.LOADING)
        private set
    val blacklist = mutableStateListOf<Blacklist>()

    //region Snackbar
    private val snackbarMutex = Mutex()
    private val snackbarMessages = ArrayList<Pair<@StringRes Int, SnackbarDuration>>()
    var snackbarShowing by mutableStateOf(false)
        private set
    var snackbarMessage by mutableStateOf<Pair<@StringRes Int, SnackbarDuration>?>(null)
        private set

    private suspend fun addSnackbarMessageInternal(
        @StringRes resourceId: Int,
        duration: SnackbarDuration
    ) {
        snackbarMutex.withLock {
            snackbarMessages.add(resourceId to duration)
            if (snackbarMessage == null) {
                snackbarMessage = snackbarMessages[0]
            }
        }
    }

    fun addSnackbarMessage(@StringRes resourceId: Int, duration: SnackbarDuration) {
        viewModelScope.launch {
            addSnackbarMessageInternal(resourceId, duration)
        }
    }

    fun notifySnackbarMessageWillDisplay() {
        if (snackbarShowing) {
            Log.w(TAG, "Snackbar behavior may be unpredictable")
            Log.w(TAG, "notifySnackbarMessageWillDisplay called when snackbarShowing is true")
        }
        snackbarShowing = true
    }

    suspend fun notifySnackbarMessageDisplayed() {
        if (snackbarMessages.isEmpty()) {
            Log.w(TAG, "notifySnackbarMessageDisplayed called, but no snackbar messages available")
            return
        }
        snackbarMutex.withLock {
            snackbarShowing = false
            snackbarMessages.removeAt(0)
            snackbarMessage = if (snackbarMessages.isNotEmpty()) snackbarMessages[0] else null
        }

    }
    //endregion

    private fun loadAuthDataFromDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            val auth = database.authDao().get()
            if (auth == null) {
                authState = AuthState.NO_DATA
            } else {
                authState = try {
                    if (Api.checkCredentials(auth.login, auth.apiKey)) {
                        // Do not synchronize blacklist
                        AuthState.AUTHORIZED
                    } else {
                        try {
                            database.authDao().logout()
                        } catch (e: Throwable) {
                            Log.e(
                                TAG,
                                "Unknown exception occurred while purging invalid auth data",
                                e
                            )
                        }
                        AuthState.NO_DATA
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "IO Error while trying to check credentials", e)
                    addSnackbarMessageInternal(R.string.network_error, SnackbarDuration.Long)
                    AuthState.IO_ERROR
                } catch (e: Throwable) {
                    AuthState.NO_DATA
                }
            }
        }
    }

    fun authenticate(
        login: String,
        apiKey: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            authState = AuthState.LOADING
            authState = try {
                if (Api.checkCredentials(login, apiKey)) {
                    updateAuthData(login, apiKey)
                    updateBlacklistFromAccount()
                    onSuccess()
                    AuthState.AUTHORIZED
                } else {
                    addSnackbarMessageInternal(
                        R.string.authentication_error,
                        SnackbarDuration.Long
                    )
                    AuthState.UNAUTHORIZED
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO Error while trying to check credentials", e)
                addSnackbarMessageInternal(R.string.network_error, SnackbarDuration.Long)
                AuthState.IO_ERROR
            } catch (e: SQLiteException) {
                Log.e(TAG, "SQL Error while trying to save credentials", e)
                addSnackbarMessageInternal(
                    R.string.database_error,
                    SnackbarDuration.Long
                )
                AuthState.SQL_ERROR
            }
        }
    }

    private suspend fun updateAuthData(login: String, apiKey: String) {
        val auth = database.authDao().get()
        if (auth == null) {
            database.authDao().insert(Auth(login, apiKey))
        } else {
            auth.login = login
            auth.apiKey = apiKey
            database.authDao().insert(auth)
        }
    }

    fun logout() {
        assert(authState == AuthState.AUTHORIZED)
        viewModelScope.launch {
            try {
                database.authDao().logout()
            } catch (e: SQLiteException) {
                Log.e(TAG, "SQLite Error while trying to logout", e)
                addSnackbarMessageInternal(
                    R.string.database_error,
                    SnackbarDuration.Long
                )
                return@launch
            }
            Api.logout()
            authState = AuthState.NO_DATA
        }
    }

    private suspend fun clearBlacklistLocally() {
        database.blacklistDao().clear()
        blacklist.clear()
    }

    private suspend fun updateBlacklistFromAccount(force: Boolean = false) {
        if (force) clearBlacklistLocally()
        else if (database.blacklistDao().count() != 0) return
        val entries = Api.getBlacklistedTags().map { Blacklist(query = it, enabled = true) }
        blacklist.addAll(entries)
        try {
            entries.forEach { entry -> database.blacklistDao().insert(entry) }
        } catch (e: SQLiteException) {
            Log.e(TAG, "SQLite Error while trying to add tag to blacklist", e)
            addSnackbarMessageInternal(
                R.string.database_error,
                SnackbarDuration.Long
            )
        }

    }
}