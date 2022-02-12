package ru.herobrine1st.e621.ui.screen.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.preference.BLACKLIST_ENABLED
import ru.herobrine1st.e621.preference.dataStore
import ru.herobrine1st.e621.ui.component.SettingLinkWithSwitch
import ru.herobrine1st.e621.ui.screen.Screens

@Composable
fun <R> Context.getPreference(key: Preferences.Key<R>, defaultValue: R): R =
    this.dataStore.data.map { it[key] ?: defaultValue }.collectAsState(initial = defaultValue).value

suspend inline fun <R> Context.setPreference(key: Preferences.Key<R>, value: R) {
    this.dataStore.edit { it[key] = value }
}

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