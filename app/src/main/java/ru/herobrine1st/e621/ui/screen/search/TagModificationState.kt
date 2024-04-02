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

package ru.herobrine1st.e621.ui.screen.search

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
sealed interface TagModificationState {

    @Serializable
    data object None : TagModificationState

    @Serializable
    data object AddingNew : TagModificationState

    @Serializable
    class Editing(val index: Int) : TagModificationState

    object Saver: androidx.compose.runtime.saveable.Saver<MutableState<TagModificationState>, String> {
        override fun restore(value: String): MutableState<TagModificationState> {
            return mutableStateOf(Json.decodeFromString(value))
        }

        override fun SaverScope.save(value: MutableState<TagModificationState>): String {
            return Json.encodeToString(value.value)
        }


    }
}