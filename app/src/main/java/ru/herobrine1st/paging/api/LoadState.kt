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

sealed interface LoadState {
    /**
     * State is not initialized and no requests are in fly
     *
     * - For [LoadStates.append] and [LoadStates.prepend] it means that first refresh isn't [Complete].
     * - For [LoadStates.refresh] it is a special case for brief initialization period, possible
     * only if paging is not started immediately (synchronously). Indicates that [PagingItems.refresh] request will do nothing.
     */
    data object Idle : LoadState

    /**
     * State is initialized and no requests are in fly
     *
     * - For [LoadStates.append] and [LoadStates.prepend] it means that additional pages can be fetched but it isn't requested
     * - For [LoadStates.refresh] it means that paging isn't started yet. [LoadStates.refresh] can't be [NotLoading] once it's not [NotLoading]
     */
    data object NotLoading : LoadState {
        operator fun invoke(endOfPaginationReached: Boolean) = when (endOfPaginationReached) {
            true -> Complete
            false -> NotLoading
        }
    }

    /**
     * State is initialized
     *
     * - For [LoadStates.append] and [LoadStates.prepend] it means that end of pagination is reached and no additional pages can be loaded
     * - For [LoadStates.refresh] it means that refresh is [Complete]
     */
    data object Complete : LoadState

    /**
     * State is initialized and [PagingSource] is performing request
     */
    data object Loading : LoadState

    /**
     * State is initialized, but [PagingSource] couldn't fetch pages. This state is recoverable.
     */
    data class Error(val throwable: Throwable) : LoadState
}