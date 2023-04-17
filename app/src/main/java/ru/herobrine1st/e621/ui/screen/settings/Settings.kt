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

package ru.herobrine1st.e621.ui.screen.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.preference.updatePreferences
import ru.herobrine1st.e621.ui.component.preferences.SettingLink
import ru.herobrine1st.e621.ui.component.preferences.SettingLinkWithSwitch
import ru.herobrine1st.e621.ui.component.preferences.SettingSwitch
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffold
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffoldState
import ru.herobrine1st.e621.ui.dialog.AlertDialog
import ru.herobrine1st.e621.ui.dialog.DisclaimerDialog
import ru.herobrine1st.e621.ui.screen.settings.component.ProxyDialog
import ru.herobrine1st.e621.util.restart

@Composable
fun Settings(
    mainScaffoldState: MainScaffoldState,
    onNavigateToBlacklistSettings: () -> Unit, onNavigateToAbout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // State
    val preferences = LocalPreferences.current
    var showPrivacyModeDialog by remember { mutableStateOf(false) }
    var showSafeModeDisclaimer by remember { mutableStateOf(false) }
    var showProxySettingsDialog by remember { mutableStateOf(false) }

    // UI
    MainScaffold(
        state = mainScaffoldState,
        title = { Text(stringResource(R.string.settings)) },
    ) {
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
                },
                onClick = onNavigateToBlacklistSettings
            )

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
                        if (!preferences.privacyModeDisclaimerShown && enabled)
                            showPrivacyModeDialog = true
                    }
                }
            )

            SettingSwitch(
                checked = preferences.safeModeEnabled,
                title = stringResource(R.string.settings_safe_mode),
                subtitle = stringResource(R.string.settings_safe_mode_shortdesc),
                icon = Icons.Default.Explicit,
                onCheckedChange = { enabled: Boolean ->
                    if (!enabled && !preferences.safeModeDisclaimerShown) showSafeModeDisclaimer =
                        true
                    else coroutineScope.launch {
                        context.updatePreferences {
                            safeModeEnabled = enabled
                        }
                    }
                }
            )

            SettingLinkWithSwitch(
                checked = preferences.hasProxy() && preferences.proxy.enabled,
                title = stringResource(
                    R.string.proxy_server
                ),
                subtitle = if (preferences.hasProxy()) with(preferences.proxy) {
                    "${type.toString().lowercase()}://$hostname:$port"
                } else "",
                icon = Icons.Default.Public,
                onCheckedChange = {
                    if (!preferences.hasProxy() && it) showProxySettingsDialog = true
                    else coroutineScope.launch {
                        context.updatePreferences {
                            proxy = proxy.toBuilder().apply {
                                enabled = it
                            }.build()
                        }
                        (context as Activity).restart()
                    }
                },
                onClick = {
                    showProxySettingsDialog = true
                }
            )

            SettingLink(
                title = stringResource(R.string.about),
                icon =  Icons.Default.Copyright,
                onClick = onNavigateToAbout
            )
        }
    }
    if (showPrivacyModeDialog) {
        AlertDialog(stringResource(R.string.privacy_mode_longdesc)) {
            showPrivacyModeDialog = false
            coroutineScope.launch {
                context.updatePreferences {
                    privacyModeDisclaimerShown = true
                }
            }
        }
    } else if (showSafeModeDisclaimer) {
        DisclaimerDialog(
            text = stringResource(R.string.settings_safe_mode_disclaimer),
            onApply = {
                showSafeModeDisclaimer = false
                coroutineScope.launch {
                    context.updatePreferences {
                        safeModeEnabled = false
                        safeModeDisclaimerShown = true
                    }
                }
            }, onDismissRequest = {
                showSafeModeDisclaimer = false
            }
        )
    } else if (showProxySettingsDialog) ProxyDialog(
        // it returns default instance if not hasProxy()
        getInitialProxy = { preferences.proxy },
        onClose = { showProxySettingsDialog = false },
        onApply = { proxy_ ->
            showProxySettingsDialog = false
            coroutineScope.launch {
                context.updatePreferences {
                    proxy = proxy_
                }
                (context as Activity).restart()
            }
        }
    )
}