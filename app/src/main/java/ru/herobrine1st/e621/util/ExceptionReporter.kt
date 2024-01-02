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

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.yield
import kotlinx.serialization.SerializationException
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import java.io.IOException

class ExceptionReporter(
    private val snackbarAdapter: SnackbarAdapter,
) {
    suspend fun handleRequestException(
        t: Throwable,
        message: String = "Unknown request exception occurred",
        showThrowable: Boolean = false,
    ) {
        if(t !is CancellationException) Log.e(TAG, message, t)
        when (t) {
            is IOException -> snackbarAdapter.enqueueMessage(
                R.string.network_error,
                SnackbarDuration.Indefinite
            )

            is SerializationException, is ContentConvertException -> snackbarAdapter.enqueueMessage(
                R.string.deserialization_error,
                SnackbarDuration.Indefinite
            )

            is CancellationException -> {
                yield() // re-throw
            }

            else -> if (showThrowable) snackbarAdapter.enqueueMessage(
                R.string.unknown_error,
                SnackbarDuration.Indefinite
            )
        }
    }


    companion object {
        const val TAG = "ExceptionReporter"
    }
}