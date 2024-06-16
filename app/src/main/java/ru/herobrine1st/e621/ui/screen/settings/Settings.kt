/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.DataSaverOff
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.settings.SettingsComponent
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.ui.component.preferences.SettingLink
import ru.herobrine1st.e621.ui.component.preferences.SettingLinkWithSwitch
import ru.herobrine1st.e621.ui.component.preferences.SettingSwitch
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState
import ru.herobrine1st.e621.ui.dialog.DisclaimerDialog
import ru.herobrine1st.e621.ui.screen.settings.component.ProxyDialog
import ru.herobrine1st.e621.util.restart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    screenSharedState: ScreenSharedState,
    onNavigateToBlacklistSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    component: SettingsComponent
) {
    val coroutineScope = rememberCoroutineScope()

    // State
    val preferences = LocalPreferences.current
    var showDataSaverModeDialog by remember { mutableStateOf(false) }
    var showSafeModeDisclaimer by remember { mutableStateOf(false) }
    var showProxySettingsDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.settings))
                },
                actions = {
                    ActionBarMenu(
                        onNavigateToSettings = screenSharedState.goToSettings,
                        onOpenBlacklistDialog = screenSharedState.openBlacklistDialog
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = screenSharedState.snackbarHostState)
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            item {
                SettingLinkWithSwitch(
                    checked = preferences.blacklistEnabled,
                    title = stringResource(R.string.setting_blacklist),
                    subtitle = stringResource(R.string.setting_blacklist_subtitle),
                    icon = Icons.Default.Block,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            component.updatePreferences {
                                copy(blacklistEnabled = enabled)
                            }
                        }
                    },
                    onClick = onNavigateToBlacklistSettings
                )
            }
            item {
                SettingSwitch(
                    checked = preferences.dataSaverModeEnabled,
                    title = stringResource(R.string.data_saver_mode),
                    subtitle = stringResource(R.string.data_saver_mode_subtitle),
                    // TODO propagate all parameters as composables and use cross-fade
                    icon = Icons.Default.DataSaverOff,
                    onCheckedChange = { checked: Boolean ->
                        if (preferences.dataSaverDisclaimerShown) {
                            coroutineScope.launch {
                                component.updatePreferences {
                                    copy(dataSaverModeEnabled = checked)
                                }
                            }
                        } else if (checked) {
                            showDataSaverModeDialog = true
                        }
                    }
                )
            }
            item {
                SettingSwitch(
                    checked = preferences.autocompleteEnabled,
                    title = stringResource(R.string.settings_search_tags_autocomplete),
                    subtitle = stringResource(R.string.settings_search_tags_autocomplete_desc),
                    icon = Icons.Default.Assistant,
                    onCheckedChange = {
                        coroutineScope.launch {
                            component.updatePreferences {
                                copy(autocompleteEnabled = it)
                            }
                        }
                    }
                )
            }
            item {
                SettingSwitch(
                    checked = preferences.safeModeEnabled,
                    title = stringResource(R.string.settings_safe_mode),
                    subtitle = stringResource(R.string.settings_safe_mode_shortdesc),
                    icon = Icons.Default.Explicit,
                    onCheckedChange = { enabled: Boolean ->
                        if (!enabled && !preferences.safeModeDisclaimerShown) showSafeModeDisclaimer =
                            true
                        else coroutineScope.launch {
                            component.updatePreferences {
                                copy(safeModeEnabled = enabled)
                            }
                        }
                    }
                )
            }
            item {
                SettingLinkWithSwitch(
                    checked = preferences.proxy != null && preferences.proxy.enabled,
                    title = stringResource(
                        R.string.proxy_server
                    ),
                    subtitle = if (preferences.proxy != null) with(preferences.proxy) {
                        "${type.toString().lowercase()}://$hostname:$port"
                    } else "",
                    icon = Icons.Default.Public,
                    onCheckedChange = {
                        if (preferences.proxy == null && it) showProxySettingsDialog = true
                        else coroutineScope.launch {
                            component.updatePreferences {
                                copy(proxy = proxy?.copy(enabled = it))
                            }
                            component.restartApplication()
                        }
                    },
                    onClick = {
                        showProxySettingsDialog = true
                    }
                )
            }
            item {
                SettingSwitch(
                    checked = preferences.autoplayOnPostOpen,
                    title = stringResource(R.string.settings_autoplay_video_on_post_open),
                    subtitle = stringResource(R.string.settings_autoplay_video_on_post_open_shortdesc),
                    icon = Icons.Default.PlayCircleOutline,
                    onCheckedChange = {
                        coroutineScope.launch {
                            component.updatePreferences {
                                copy(autoplayOnPostOpen = it)
                            }
                        }
                    }
                )
            }
            item {
                SettingLink(
                    title = stringResource(R.string.about),
                    icon = Icons.Default.Copyright,
                    onClick = onNavigateToAbout
                )
            }
        }
    }
    if (showDataSaverModeDialog) {
        DisclaimerDialog(
            title = { Text(stringResource(R.string.warning)) },
            text = { Text(stringResource(R.string.data_saver_mode_long_description)) },
            onApply = {
                showDataSaverModeDialog = false
                coroutineScope.launch {
                    component.updatePreferences {
                        copy(
                            dataSaverDisclaimerShown = true,
                            dataSaverModeEnabled = true
                        )
                    }
                }
            },
            onDismiss = {
                showDataSaverModeDialog = false
            }
        )
    } else if (showSafeModeDisclaimer) {
        DisclaimerDialog(
            text = { Text(stringResource(R.string.settings_safe_mode_disclaimer)) },
            onApply = {
                showSafeModeDisclaimer = false
                coroutineScope.launch {
                    component.updatePreferences {
                        copy(
                            safeModeEnabled = false,
                            safeModeDisclaimerShown = true
                        )
                    }
                }
            },
            onDismiss = {
                showSafeModeDisclaimer = false
            }
        )
    } else if (showProxySettingsDialog) ProxyDialog(
        // it returns default instance if not hasProxy()
        getInitialProxy = { preferences.proxy },
        onClose = { showProxySettingsDialog = false },
        onApply = { proxy ->
            showProxySettingsDialog = false
            coroutineScope.launch {
                component.updatePreferences {
                    copy(proxy = proxy)
                }
                component.restartApplication()
            }
        }
    )
}