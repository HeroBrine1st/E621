package ru.herobrine1st.e621

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.database.Database
import ru.herobrine1st.e621.entity.Auth
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.enumeration.AuthState
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class ApplicationViewModel @Inject constructor(
    private val database: Database,
    private val api: Api,
    private val snackbar: SnackbarAdapter
) : ViewModel() {

    companion object {
        val TAG = ApplicationViewModel::class.simpleName
    }

    fun loadAllFromDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            loadAuthDataFromDatabase()
        }
    }


    //region Auth
    var authState: AuthState by mutableStateOf(AuthState.LOADING)
        private set
    val login get() = api.login

    private suspend fun loadAuthDataFromDatabase() {
        val auth = database.authDao().get()
        if (auth == null) {
            authState = AuthState.NO_DATA
        } else {
            authState = try {
                if (api.checkCredentials(auth.login, auth.apiKey)) {
                    AuthState.AUTHORIZED
                } else {
                    try {
                        database.authDao().logout()
                    } catch (e: Throwable) {
                        Log.e(
                            TAG, "Unknown exception occurred while purging invalid auth data",
                            e
                        )
                    }
                    AuthState.NO_DATA
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO Error while trying to check credentials", e)
                snackbar.enqueueMessage(R.string.network_error, SnackbarDuration.Long)
                AuthState.IO_ERROR
            } catch (e: Throwable) {
                AuthState.NO_DATA
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
                if (api.checkCredentials(login, apiKey)) {
                    updateAuthData(login, apiKey)
                    updateBlacklistFromAccount()
                    onSuccess()
                    AuthState.AUTHORIZED
                } else {
                    snackbar.enqueueMessage(
                        R.string.authentication_error,
                        SnackbarDuration.Long
                    )
                    AuthState.UNAUTHORIZED
                }
            } catch (e: IOException) {
                Log.e(TAG, "IO Error while trying to check credentials", e)
                snackbar.enqueueMessage(R.string.network_error, SnackbarDuration.Long)
                AuthState.IO_ERROR
            } catch (e: SQLiteException) {
                Log.e(TAG, "SQL Error while trying to save credentials", e)
                snackbar.enqueueMessage(
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
                snackbar.enqueueMessage(
                    R.string.database_error,
                    SnackbarDuration.Long
                )
                return@launch
            }
            api.logout()
            authState = AuthState.NO_DATA
        }
    }

    //endregion
    //region Blacklist

    private suspend fun updateBlacklistFromAccount() {
        if (database.blacklistDao().count() != 0) return
        val tags = api.getBlacklistedTags()
        database.withTransaction {
            tags.forEach {
                database.blacklistDao().insert(BlacklistEntry(it, true))
            }
        }
    }
//    //region Up/down votes
//
//    suspend fun vote(post: Post, vote: Int) {
//        assert(vote in -1..1)
//        val currentVote = database.voteDao().getVote(post.id) ?: 0
//        if (vote == 0) {
//            val score = api.vote(post.id, currentVote, false)
//            if (score.ourScore != 0) { // API does not send user's vote with post
//                assert(api.vote(post.id, score.ourScore, false).ourScore == 0)
//            }
//        } else {
//            assert(api.vote(post.id, vote, true).ourScore == vote)
//        }
//        database.voteDao().insertOrUpdate(post.id, vote)
//    }
//
//    suspend fun getPostVote(post: Post): Int {
//        return database.voteDao().getVote(post.id) ?: 0
//    }
//
//    //endregion
}