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

package ru.herobrine1st.e621.ui.component.legal

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R

@Composable
fun LicenseAndDisclaimerInitialDialogs(
    hasShownBefore: Boolean,
    onCompletion: () -> Unit
) {
    // Why: I want users to know that this application is free software
    // Also non-affiliation disclaimer may be useful.. sometimes
    val context = LocalContext.current

    var showLicenseDialog by remember(hasShownBefore) { mutableStateOf(!hasShownBefore) }
    var showNonAffiliationDisclaimerDialog by remember { mutableStateOf(false) }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = {
                Toast.makeText(context, R.string.explicitly_click_button, Toast.LENGTH_SHORT).show()
            },
            title = {
                Text(stringResource(R.string.license_word))
            },
            text = {
                Text(stringResource(R.string.license_brief))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLicenseDialog = false
                        showNonAffiliationDisclaimerDialog = true
                    }
                ) {
                    Text(stringResource(R.string.i_understand))
                }
            }
        )
    }
    if (showNonAffiliationDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = {
                Toast.makeText(context, R.string.explicitly_click_button, Toast.LENGTH_SHORT).show()
            },
            title = {
                Text(stringResource(R.string.disclaimer))
            },
            text = {
                Text(stringResource(R.string.non_affiliation_disclaimer))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNonAffiliationDisclaimerDialog = false
                        onCompletion()
                    }
                ) {
                    Text(stringResource(R.string.i_understand))
                }
            }
        )
    }
}