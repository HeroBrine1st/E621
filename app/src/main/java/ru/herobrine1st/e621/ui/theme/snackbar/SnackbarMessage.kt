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

package ru.herobrine1st.e621.ui.theme.snackbar

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.MutableSharedFlow

data class SnackbarMessage(
    @StringRes val stringId: Int,
    val withDismissAction: Boolean = false,
    val duration: SnackbarDuration,
    val formatArgs: Array<out Any>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SnackbarMessage

        if (stringId != other.stringId) return false
        if (duration != other.duration) return false
        return formatArgs.contentEquals(other.formatArgs)
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
    withDismissAction: Boolean = true,
    vararg formatArgs: Any
) {
    this.emit(
        SnackbarMessage(resourceId, withDismissAction, duration, formatArgs)
    )
}