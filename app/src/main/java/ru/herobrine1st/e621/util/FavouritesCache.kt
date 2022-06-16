package ru.herobrine1st.e621.util

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * To synchronize many screens.
 */
@ActivityRetainedScoped
class FavouritesCache @Inject constructor() {
    private val _flow = MutableStateFlow<Map<Int, Boolean>>(mapOf()) // id to isFavourite

    val flow: StateFlow<Map<Int, Boolean>> = _flow

    suspend fun setFavourite(id: Int, isFavourite: Boolean) =
        _flow.emit(_flow.value.toMutableMap().apply { put(id, isFavourite) })
}