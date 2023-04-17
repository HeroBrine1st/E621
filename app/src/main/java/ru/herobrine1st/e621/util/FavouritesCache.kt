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

package ru.herobrine1st.e621.util

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import ru.herobrine1st.e621.api.model.Post
import javax.inject.Inject

/**
 * To synchronize many screens.
 */
@ActivityRetainedScoped
class FavouritesCache @Inject constructor() {
    private val _flow = MutableStateFlow<Map<Int, Boolean>>(mapOf()) // id to isFavourite

    val flow = _flow.asStateFlow()

    fun setFavourite(id: Int, isFavourite: Boolean) {
        _flow.getAndUpdate {
            it + (id to isFavourite)
        }
    }

    fun isFavourite(post: Post) = flow.value.getOrDefault(post.id, post.isFavorited)

}