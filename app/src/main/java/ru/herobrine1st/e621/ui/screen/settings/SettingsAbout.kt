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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffold
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffoldState
import ru.herobrine1st.e621.ui.component.scaffold.rememberPreviewMainScaffoldState
import ru.herobrine1st.e621.util.PreviewUtils

@Composable
fun SettingsAbout(
    mainScaffoldState: MainScaffoldState,
    navigateToLicense: () -> Unit,
    navigateToOssLicenses: () -> Unit,
) {
    MainScaffold(
        state = mainScaffoldState,
        title = { Text(stringResource(R.string.about)) },
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {} // "padding"
            item {
                Card(Modifier.padding(horizontal = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            Image(
                                painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(128.dp)
                            )
                        }
                        Column {
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.subtitle1
                            )
                            Spacer(modifier = Modifier.size(2.dp))
                            Text(
                                stringResource(
                                    R.string.app_description,
                                    BuildConfig.DEEP_LINK_BASE_URL
                                ),
                                Modifier.alpha(ContentAlpha.medium),
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.disclaimer),
                            style = MaterialTheme.typography.h6
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.non_affiliation_disclaimer))
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.license_word),
                            style = MaterialTheme.typography.h6
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.license_brief))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = navigateToOssLicenses) {
                                Text(stringResource(R.string.oss_licenses))
                            }
                            TextButton(
                                onClick = navigateToLicense
                            ) {
                                Text(stringResource(R.string.license_name))
                            }
                        }
                    }
                }
            }
            item {}
        }
    }
}

@Preview
@Composable
@OptIn(PreviewUtils::class)
fun SettingsAbout() {
    SettingsAbout(rememberPreviewMainScaffoldState(), {}, {})
}