package ru.herobrine1st.e621.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.preference.*
import ru.herobrine1st.e621.ui.component.preferences.SettingLinkWithSwitch
import ru.herobrine1st.e621.ui.component.preferences.SettingSwitch
import ru.herobrine1st.e621.ui.dialog.AlertDialog
import ru.herobrine1st.e621.ui.screen.Screens

@Composable
fun Settings(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Preferences
    val isBlacklistEnabled by context.getPreference(BLACKLIST_ENABLED, true)
    val isPrivacyModeEnabled by context.getPreference(PRIVACY_MODE, false)
    val hasShownPrivacyModeDisclaimer by context.getPreference(PRIVACY_MODE_DIALOG_SHOWN, false)

    // State
    var showPrivacyModeDialog by remember { mutableStateOf(false)}

    // Composition
    Column {
        SettingLinkWithSwitch(
            checked = isBlacklistEnabled,
            title = stringResource(R.string.setting_blacklist),
            subtitle = stringResource(R.string.setting_blacklist_subtitle),
            icon = Icons.Default.Block,
            onCheckedChange = {
                coroutineScope.launch {
                    context.setPreference(BLACKLIST_ENABLED, it)
                }
            }
        ) {
            navController.navigate(Screens.SettingsBlacklist.route)
        }

        SettingSwitch(
            checked = isPrivacyModeEnabled,
            title = stringResource(R.string.privacy_mode),
            subtitle = stringResource(R.string.privacy_mode_subtitle),
            icon = Icons.Default.Shield,
            onCheckedChange = {
                coroutineScope.launch {
                    context.setPreference(PRIVACY_MODE, it)
                    if(!hasShownPrivacyModeDisclaimer && it) showPrivacyModeDialog = true
                }
            }
        )

    }
    if(showPrivacyModeDialog) {
        AlertDialog(stringResource(R.string.privacy_mode_longdesc)) {
            showPrivacyModeDialog = false
            coroutineScope.launch {
                context.setPreference(PRIVACY_MODE_DIALOG_SHOWN, true)
            }
        }
    }
}