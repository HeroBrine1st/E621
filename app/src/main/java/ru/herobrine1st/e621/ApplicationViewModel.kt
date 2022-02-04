package ru.herobrine1st.e621

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
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
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(UserAgentInterceptor(BuildConfig.USER_AGENT))
        .build()

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
                            AuthState.SQL_ERROR
                        }
                    } else {
                        AuthState.UNAUTHORIZED
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO Error while trying to check credentials", e)
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