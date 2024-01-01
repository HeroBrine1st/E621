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

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter


class AuthorizationNotifier(
    private val snackbarAdapter: SnackbarAdapter
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    fun notifyAuthorizationRevoked() {
        coroutineScope.launch {
            snackbarAdapter.enqueueMessage(R.string.auth_revoked, SnackbarDuration.Indefinite)
        }
    }
}