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

package ru.herobrine1st.paging.api

import androidx.compose.runtime.Stable

@Stable
interface PagingItems<Value : Any> {

    val loadStates: LoadStates

    fun refresh()


    /**
     * Triggers retrying on any error state
     */
    fun retry()

    val size: Int

    /**
     * A linear snapshot of loaded items. Accessing this list does not trigger page fetching.
     *
     * Supports random access
     */
    val items: List<Value>

    /**
     * Returns element with specified index and triggers page fetching if necessary
     */
    operator fun get(index: Int): Value

    /**
     * Equivalent of accessing [items]
     */
    fun peek(index: Int): Value
}

inline fun <Value : Any, T> PagingItems<Value>.itemKey(crossinline block: (Value) -> T): (Int) -> T =
    {
        block(items[it])
    }

inline fun <Value : Any, T> PagingItems<Value>.contentType(crossinline block: (Value) -> T?): (Int) -> T? =
    {
        block(items[it])
    }