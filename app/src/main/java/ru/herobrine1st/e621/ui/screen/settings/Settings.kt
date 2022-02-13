package ru.herobrine1st.e621.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.preference.BLACKLIST_ENABLED
import ru.herobrine1st.e621.preference.getPreference
import ru.herobrine1st.e621.preference.setPreference
import ru.herobrine1st.e621.ui.component.SettingLinkWithSwitch
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
    }
}