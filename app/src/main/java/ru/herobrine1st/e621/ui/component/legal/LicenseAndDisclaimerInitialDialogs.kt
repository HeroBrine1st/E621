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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.dialog.ActionDialog

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
        val uriHandler = LocalUriHandler.current
        val licenseUrl = stringResource(R.string.license_url)
        ActionDialog(
            title = stringResource(R.string.license_word),
            onDismissRequest = {
                Toast.makeText(context, R.string.explicitly_click_button, Toast.LENGTH_SHORT).show()
            },
            actions = {
                TextButton(
                    onClick = {
                        uriHandler.openUri(licenseUrl)
                    }
                ) {
                    Text(stringResource(R.string.license_name_short))
                    Icon(
                        Icons.Default.OpenInBrowser,
                        contentDescription = stringResource(R.string.open_in_browser)
                    )
                }
                TextButton(
                    onClick = {
                        showLicenseDialog = false
                        showNonAffiliationDisclaimerDialog = true
                    }
                ) {
                    Text(stringResource(R.string.i_understand))
                }
            }
        ) {
            Text(stringResource(R.string.license_brief))
        }
    }
    if (showNonAffiliationDisclaimerDialog) {
        ActionDialog(
            title = stringResource(R.string.disclaimer),
            onDismissRequest = {
                Toast.makeText(context, R.string.explicitly_click_button, Toast.LENGTH_SHORT).show()
            },
            actions = {
                TextButton(
                    onClick = {
                        showNonAffiliationDisclaimerDialog = false
                        onCompletion()
                    }
                ) {
                    Text(stringResource(R.string.i_understand))
                }
            }
        ) {
            Text(stringResource(R.string.non_affiliation_disclaimer))
        }
    }
}