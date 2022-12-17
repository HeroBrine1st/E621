/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.ui.snackbar

import androidx.annotation.StringRes
import androidx.compose.material.SnackbarDuration
import kotlinx.coroutines.flow.MutableSharedFlow

data class SnackbarMessage(
    @StringRes val stringId: Int,
    val duration: SnackbarDuration,
    val formatArgs: Array<out Any>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SnackbarMessage

        if (stringId != other.stringId) return false
        if (duration != other.duration) return false
        if (!(formatArgs.contentEquals(other.formatArgs))) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stringId
        result = 31 * result + duration.hashCode()
        result = 31 * result + formatArgs.contentHashCode()
        return result
    }
}

suspend fun MutableSharedFlow<SnackbarMessage>.enqueueMessage(
    @StringRes resourceId: Int,
    duration: SnackbarDuration = SnackbarDuration.Long,
    vararg formatArgs: Any
) {
    this.emit(
        SnackbarMessage(resourceId, duration, formatArgs)
    )
}