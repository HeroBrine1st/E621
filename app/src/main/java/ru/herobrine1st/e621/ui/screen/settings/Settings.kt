package ru.herobrine1st.e621.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
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
    Column {
        SettingLinkWithSwitch(
            checked = context.getPreference(BLACKLIST_ENABLED, true),
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

        val privacyModeDialogShown = context.getPreference(PRIVACY_MODE_DIALOG_SHOWN, false)
        var showPrivacyModeDialog by remember { mutableStateOf(false)}
        SettingSwitch(
            checked = context.getPreference(PRIVACY_MODE, false),
            title = stringResource(R.string.privacy_mode),
            subtitle = stringResource(R.string.privacy_mode_subtitle),
            icon = Icons.Default.Shield,
            onCheckedChange = {
                coroutineScope.launch {
                    context.setPreference(PRIVACY_MODE, it)
                    if(!privacyModeDialogShown && it) showPrivacyModeDialog = true
                }
            }
        )
        if(showPrivacyModeDialog) {
            AlertDialog(stringResource(R.string.privacy_mode_longdesc)) {
                showPrivacyModeDialog = false
                coroutineScope.launch {
                    context.setPreference(PRIVACY_MODE_DIALOG_SHOWN, true)
                }
            }
        }
    }
}