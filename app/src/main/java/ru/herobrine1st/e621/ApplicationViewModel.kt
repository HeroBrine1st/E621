package ru.herobrine1st.e621

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.entity.Auth
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.enumeration.AuthState
import ru.herobrine1st.e621.ui.SnackbarMessage
import ru.herobrine1st.e621.util.StatefulBlacklistEntry
import ru.herobrine1st.e621.util.getAllAsStateful
import java.io.IOException
import java.util.function.Predicate


class ApplicationViewModel(val database: Database, val api: Api) : ViewModel() {
    class Factory(val database: Database, val api: Api) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ApplicationViewModel(database, api) as T
        }
    }

    companion object {
        val TAG = ApplicationViewModel::class.simpleName
    }

    fun loadAllFromDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            loadAuthDataFromDatabase()
            loadBlacklistLocally()
        }
    }

    //region Snackbar
    private val snackbarMutex = Mutex()
    private val snackbarMessages = ArrayList<SnackbarMessage>()
    var snackbarShowing by mutableStateOf(false)
        private set
    var snackbarMessage by mutableStateOf<SnackbarMessage?>(null)
        private set

    private suspend fun addSnackbarMessageInternal(
        @StringRes resourceId: Int,
        duration: SnackbarDuration,
        vararg formatArgs: Any
    ) {
        snackbarMutex.withLock {
            snackbarMessages.add(SnackbarMessage(resourceId, duration, formatArgs))
            if (snackbarMessage == null) {
                snackbarMessage = snackbarMessages[0]
            }
        }
    }

    fun addSnackbarMessage(
        @StringRes resourceId: Int,
        duration: SnackbarDuration,
        vararg formatArgs: Any
    ) {
        viewModelScope.launch {
            addSnackbarMessageInternal(resourceId, duration, *formatArgs)
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
                addSnackbarMessageInternal(R.string.network_error, SnackbarDuration.Long)
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
            api.logout()
            authState = AuthState.NO_DATA
        }
    }

    //endregion
    //region Blacklist
    val blacklistDoNotUseAsFilter =
        mutableStateListOf<StatefulBlacklistEntry>() // This list doesn't change when user enables/disables entries
    var blacklistPostPredicate by mutableStateOf<Predicate<Post>>(Predicate { true }) // This state does

    var blacklistLoading by mutableStateOf(true)
        private set
    var blacklistUpdating by mutableStateOf(false)
        private set

    private suspend fun clearBlacklistLocally() {
        database.blacklistDao().clear()
        blacklistDoNotUseAsFilter.clear()
        blacklistPostPredicate = Predicate<Post> { false }
    }

    private fun updateFilteringBlacklistEntriesList() {
        blacklistPostPredicate = blacklistDoNotUseAsFilter
            .filter { it.enabled }
            .map { it.predicate }
            .reduceOrNull { acc, predicate -> acc.or(predicate) }
            ?: Predicate { false }
    }

    private suspend fun updateBlacklistFromAccount(force: Boolean = false) {
        if (force) clearBlacklistLocally()
        else if (database.blacklistDao().count() != 0) return
        assert(blacklistDoNotUseAsFilter.size == 0)
        blacklistLoading = true
        val entries = api.getBlacklistedTags().map { BlacklistEntry(query = it, enabled = true) }
        try {
            database.blacklistDao().apply {
                entries.forEach { entry -> insert(entry) }
            }
        } catch (e: SQLiteException) {
            Log.e(TAG, "SQLite Error while trying to add tag to blacklist", e)
            addSnackbarMessageInternal(
                R.string.database_error,
                SnackbarDuration.Long
            )
            return
        } finally {
            blacklistLoading = false
        }
        entries.mapTo(blacklistDoNotUseAsFilter) { StatefulBlacklistEntry.of(it) }
        updateFilteringBlacklistEntriesList()
    }

    private suspend fun loadBlacklistLocally() {
        if (blacklistDoNotUseAsFilter.isNotEmpty()) return // Already loaded, don't need to do it again
        blacklistLoading = true
        try {
            val entries = database.blacklistDao().getAllAsStateful()
            blacklistDoNotUseAsFilter.clear()
            blacklistDoNotUseAsFilter.addAll(entries)
            updateFilteringBlacklistEntriesList()
        } catch (e: SQLiteException) {
            Log.e(TAG, "SQLite Error while trying to load blacklist from database", e)
            addSnackbarMessageInternal(
                R.string.database_error,
                SnackbarDuration.Long
            )
        } finally {
            blacklistLoading = false
        }
    }

    suspend fun applyBlacklistChanges() {
        if (blacklistUpdating) {
            Log.w(TAG, "applyBlacklistChanges called again, but last call has not ended")
            return
        }
        blacklistUpdating = true
        for (entry in blacklistDoNotUseAsFilter) {
            try {
                entry.applyChanges(database)
            } catch (e: SQLiteException) {
                Log.e(TAG, "SQLite Error while trying to update blacklist entry", e)
                addSnackbarMessageInternal(
                    R.string.database_error_updating_blacklist,
                    SnackbarDuration.Long,
                    entry.query
                )
                blacklistDoNotUseAsFilter.forEach { it.resetChanges() }
                blacklistDoNotUseAsFilter.removeIf { it.isPendingInsertion }
                break
            }
        }
        blacklistDoNotUseAsFilter.removeIf { it.isPendingDeletion }
        updateFilteringBlacklistEntriesList()
        blacklistUpdating = false
    }

    fun resetBlacklistEntry(entry: StatefulBlacklistEntry) {
        if (entry.isPendingInsertion) blacklistDoNotUseAsFilter.remove(entry)
        else entry.resetChanges()
    }

    fun deleteBlacklistEntry(entry: StatefulBlacklistEntry) {
        if (entry.isPendingInsertion) blacklistDoNotUseAsFilter.remove(entry)
        else entry.markAsDeleted()
    }

    fun addBlacklistEntry(query: String) {
        blacklistDoNotUseAsFilter.add(StatefulBlacklistEntry.create(query))
    }

    fun resetBlacklistChanges() {
        blacklistDoNotUseAsFilter.removeIf { it.isPendingInsertion }
        blacklistDoNotUseAsFilter.forEach { it.resetChanges() }
    }

    //endregion
    //region Favorites

    private val favoritesCache = mutableStateMapOf<Int, Boolean>()

    fun isFavorited(post: Post): Boolean {
        return favoritesCache.getOrDefault(post.id, post.isFavorited)
    }


    fun handleFavoritePost(post: Post) {
        val isFavorited = isFavorited(post)
        val isCached = post.id in favoritesCache
        favoritesCache[post.id] = !isFavorited
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isFavorited) api.deleteFavorite(post.id)
                else api.favorite(post.id)
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "IO Error while while trying to (un)favorite post (id=${post.id}, isFavorited=$isFavorited)",
                    e
                )
                addSnackbarMessageInternal(R.string.network_error, SnackbarDuration.Long)
                if (isCached) favoritesCache[post.id] = isFavorited
                else favoritesCache.remove(post.id)
            }
        }
    }
    //endregion
    //region Up/down votes

    suspend fun vote(post: Post, vote: Int) {
        assert(vote in -1..1)
        val currentVote = database.voteDao().getVote(post.id) ?: 0
        if (vote == 0) {
            val score = api.vote(post.id, currentVote, false)
            if (score.ourScore != 0) { // API does not send user's vote with post
                assert(api.vote(post.id, score.ourScore, false).ourScore == 0)
            }
        } else {
            assert(api.vote(post.id, vote, true).ourScore == vote)
        }
        database.voteDao().insertOrUpdate(post.id, vote)
    }

    suspend fun getPostVote(post: Post): Int {
        return database.voteDao().getVote(post.id) ?: 0
    }

    //endregion
}