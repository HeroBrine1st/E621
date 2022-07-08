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
import ru.herobrine1st.e621.preference.getPreferencesAsState
import ru.herobrine1st.e621.preference.updatePreferences
import ru.herobrine1st.e621.ui.component.preferences.SettingLinkWithSwitch
import ru.herobrine1st.e621.ui.component.preferences.SettingSwitch
import ru.herobrine1st.e621.ui.dialog.AlertDialog
import ru.herobrine1st.e621.ui.screen.Screen

@Composable
fun Settings(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val preferences by context.getPreferencesAsState()

    // State
    var showPrivacyModeDialog by remember { mutableStateOf(false) }

    // Composition
    Column {
        SettingLinkWithSwitch(
            checked = preferences.blacklistEnabled,
            title = stringResource(R.string.setting_blacklist),
            subtitle = stringResource(R.string.setting_blacklist_subtitle),
            icon = Icons.Default.Block,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    context.updatePreferences {
                        blacklistEnabled = enabled
                    }
                }
            }
        ) {
            navController.navigate(Screen.SettingsBlacklist.route)
        }

        SettingSwitch(
            checked = preferences.privacyModeEnabled,
            title = stringResource(R.string.privacy_mode),
            subtitle = stringResource(R.string.privacy_mode_subtitle),
            icon = Icons.Default.Shield,
            onCheckedChange = { enabled: Boolean ->
                coroutineScope.launch {
                    context.updatePreferences {
                        privacyModeEnabled = enabled
                    }
                    if (!preferences.privacyModeDialogWasShown && enabled) showPrivacyModeDialog = true
                }
            }
        )

    }
    if (showPrivacyModeDialog) {
        AlertDialog(stringResource(R.string.privacy_mode_longdesc)) {
            showPrivacyModeDialog = false
            coroutineScope.launch {
                context.updatePreferences {
                    privacyModeDialogWasShown = true
                }
            }
        }
    }
}