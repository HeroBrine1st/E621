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

package ru.herobrine1st.e621.navigation.component.post

import androidx.compose.runtime.Stable
import ru.herobrine1st.e621.api.model.Pool
import ru.herobrine1st.e621.api.model.PoolId

interface PoolsComponent {
    @Stable
    val pools: List<PoolState>

    @Stable
    val showPools: Boolean

    @Stable
    val loadingPools: Boolean

    fun openPool(pool: Pool)

    fun dismiss()

    sealed interface PoolState {
        val id: PoolId

        data class Successful(val pool: Pool) : PoolState {
            override val id by pool::id
        }

        data class Error(override val id: PoolId) : PoolState

        data class NotLoaded(override val id: PoolId) : PoolState
    }
}

