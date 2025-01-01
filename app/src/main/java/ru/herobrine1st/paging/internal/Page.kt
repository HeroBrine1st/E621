/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.paging.internal

import kotlinx.serialization.Serializable
import ru.herobrine1st.paging.api.LoadResult

// A page with associated key
@Serializable
data class Page<Key : Any, Value : Any>(
    val key: Key,
    val nextKey: Key?,
    val prevKey: Key?,
    val data: List<Value>,
) {
    companion object {
        fun <Key : Any, Value : Any> from(
            loadResult: LoadResult.Page<Key, Value>,
            key: Key,
        ): Page<Key, Value> = Page(
            key = key,
            nextKey = loadResult.nextKey,
            prevKey = loadResult.previousKey,
            data = loadResult.data
        )
    }
}