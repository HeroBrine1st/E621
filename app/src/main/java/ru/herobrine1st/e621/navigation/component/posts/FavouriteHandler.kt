/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.navigation.component.posts

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.ApiException
import ru.herobrine1st.e621.api.ensureSuccessful
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.FavouritesCache
import java.io.IOException

private const val TAG = "IFavouriteHandlerComponent"

// favourite change = post is changed from favourite to non-favourite or vise versa,
// not "change of the most favourite post" (and even this has dual meaning.. ironic)
// if you have better name - let me know
// The same with package for this function..
suspend fun handleFavouriteChange(
    favouritesCache: FavouritesCache,
    api: API,
    snackbar: SnackbarAdapter,
    post: Post
) {
    val wasFavourite: FavouritesCache.FavouriteState = favouritesCache.isFavourite(post)
    when (wasFavourite) {
        is FavouritesCache.FavouriteState.InFly -> {
            Log.w(
                TAG,
                "Inconsistent state: \"favourite\" button was clicked while should be disabled"
            )
            return
        }
        // Smart cast is not that smart
        is FavouritesCache.FavouriteState.Determined -> {}
    }
    // Instant UI reaction
    favouritesCache.setFavourite(post.id, FavouritesCache.FavouriteState.InFly(wasFavourite))
    try {
        if (wasFavourite.isFavourite) api.removeFromFavourites(post.id)
            .ensureSuccessful()
        else api.addToFavourites(post.id).ensureSuccessful()
        favouritesCache.setFavourite(
            post.id,
            FavouritesCache.FavouriteState.Determined.fromBoolean(!wasFavourite.isFavourite)
        )
    } catch (e: ApiException) {
        // TODO this can also occur when post is already (un)favourite
        favouritesCache.setFavourite(post.id, wasFavourite)
        snackbar.enqueueMessage(R.string.unknown_api_error, SnackbarDuration.Long)
        Log.e(TAG, "An API exception occurred", e)
    } catch (e: IOException) {
        favouritesCache.setFavourite(post.id, wasFavourite)
        Log.e(
            TAG,
            "IO Error while while trying to (un)favorite post (id=${post.id}, wasFavourite=$wasFavourite)",
            e
        )
        snackbar.enqueueMessage(R.string.network_error, SnackbarDuration.Long)
    }
}