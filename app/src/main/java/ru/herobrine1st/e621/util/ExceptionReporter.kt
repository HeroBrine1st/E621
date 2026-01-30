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

package ru.herobrine1st.e621.util

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import io.ktor.serialization.*
import io.ktor.util.network.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.yield
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import java.io.IOException

interface ExceptionReporter {
    suspend fun handleRequestException(
        t: Throwable,
        message: String = "Unknown request exception occurred",
        dontShowSnackbar: Boolean = false,
        showThrowable: Boolean = false,
    )
}

class ExceptionReporterImpl(
    private val snackbarAdapter: SnackbarAdapter,
): ExceptionReporter {
    override suspend fun handleRequestException(
        t: Throwable,
        message: String,
        dontShowSnackbar: Boolean,
        showThrowable: Boolean
    ) {
        if(t !is CancellationException) Log.e(TAG, message, t)
        if(dontShowSnackbar) return
        when (t) {
            // TODO it suspends on queue and may block network requests in some cases (that's why "dontShowSnackbar" - to avoid suspending)
            is IOException, is UnresolvedAddressException -> snackbarAdapter.enqueueMessage(
                R.string.network_error,
                SnackbarDuration.Indefinite,
            )

            is IllegalArgumentException, is ContentConvertException -> snackbarAdapter.enqueueMessage(
                R.string.deserialization_error,
                SnackbarDuration.Indefinite,
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