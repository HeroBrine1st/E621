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