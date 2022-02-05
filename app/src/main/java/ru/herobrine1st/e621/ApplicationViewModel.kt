package ru.herobrine1st.e621

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.herobrine1st.e621.entity.Auth
import ru.herobrine1st.e621.net.UserAgentInterceptor
import ru.herobrine1st.e621.util.lateinitMutableState
import java.io.IOException

val TAG = ApplicationViewModel::class.simpleName

enum class AuthState {
    NO_DATA, // default if no auth info at the start of app
    LOADING,
    AUTHORIZED,
    IO_ERROR,
    SQL_ERROR,
    UNAUTHORIZED // error when trying to authenticate
}

class ApplicationViewModel : ViewModel() {
    lateinit var database: Database
        private set
    var auth: Auth by lateinitMutableState()
        private set
    var authState: AuthState by mutableStateOf(AuthState.LOADING)
        private set

    private val snackbarMutex = Mutex()
    private val snackbarMessages = ArrayList<Pair<@StringRes Int, SnackbarDuration>>()
    var snackbarShowing by mutableStateOf(false)
        private set
    var snackbarMessage by mutableStateOf<Pair<@StringRes Int, SnackbarDuration>?>(null)
        private set

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(UserAgentInterceptor(BuildConfig.USER_AGENT))
        .build()

    private suspend fun addSnackbarMessage(@StringRes resourceId: Int, duration: SnackbarDuration) {
        snackbarMutex.withLock {
            snackbarMessages.add(resourceId to duration)
            if (snackbarMessage == null) {
                snackbarMessage = snackbarMessages[0]
            }
        }
    }

    fun notifySnackbarMessageWillDisplay() {
        if(snackbarShowing)
            Log.w(TAG, "Snackbar behavior may be unpredictable")
            Log.w(TAG, "notifySnackbarMessageWillDisplay called when snackbarShowing is true")
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

    fun injectDatabase(db: Database) { // В Hilt ровно такая же документация, как у compose, так что нахуй его
        // К тому же Hilt требует объекты для инжекта ещё до запуска активити, а получить базу данных без активити невозможно. В документации не описан (читать как описан в дебрях документации) другой способов
        database = db
    }

    fun fetchAuthData() {
        viewModelScope.launch(Dispatchers.IO) {
            val auth = database.authDao().get()
            if (auth == null) {
                authState = AuthState.NO_DATA
            } else {
                authState = AuthState.AUTHORIZED
                this@ApplicationViewModel.auth = auth
            }
        }
    }

    fun tryAuthenticate(login: String, apiKey: String, onSuccess: () -> Unit = {}) {
        assert(authState != AuthState.LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            authState = AuthState.LOADING
            val req = Request.Builder()
                .url(
                    HttpUrl.Builder()
                        .scheme("https")
                        .host(BuildConfig.API_URL)
                        .addPathSegments("users/$login.json")
                        .build()
                )
                .header("Authorization", Credentials.basic(login, apiKey))
                .addHeader("Accept", "application/json")
                .build()
            try {
                okHttpClient.newCall(req).execute().use {
                    authState = if (it.isSuccessful) {
                        try {
                            updateAuthData(login, apiKey)
                            onSuccess()
                            AuthState.AUTHORIZED
                        } catch (e: SQLiteException) {
                            Log.e(TAG, "SQL Error while trying to save credentials", e)
                            addSnackbarMessage(R.string.database_error, SnackbarDuration.Long)
                            AuthState.SQL_ERROR
                        }
                    } else {
                        addSnackbarMessage(R.string.authentication_error, SnackbarDuration.Long)
                        AuthState.UNAUTHORIZED
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO Error while trying to check credentials", e)
                addSnackbarMessage(R.string.network_error, SnackbarDuration.Long)
                authState = AuthState.IO_ERROR
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
                database.authDao().delete(auth)
            } catch (e: SQLiteException) {
                Log.e(TAG, "IO Error while trying to logout", e)
                return@launch
                // TODO snackbar
            }
            authState = AuthState.NO_DATA
        }
    }
}