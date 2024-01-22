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

package ru.herobrine1st.e621.ui.screen.post

import ru.herobrine1st.e621.api.common.CommentData

sealed interface CommentsLoadingState {
    // To be used as content key for animation
    val index: Int

    @Suppress("SpellCheckingInspection") // idk how to name it
    sealed interface Showable : CommentsLoadingState {
        override val index: Int
            get() = 2
        val commentData: CommentData

        data class Success(override val commentData: CommentData) : Showable

        data object Loading : Showable {
            override val commentData by CommentData.Companion::PLACEHOLDER
        }
    }

    data object NotLoading : CommentsLoadingState {
        override val index: Int
            get() = 0
    }

    data object Empty : CommentsLoadingState {
        override val index: Int
            get() = 1
    }

    data object Failed : CommentsLoadingState {
        override val index: Int
            get() = 3
    }

    data object Forbidden/*due to credentials absence*/ : CommentsLoadingState {
        override val index: Int
            get() = 4
    }

}