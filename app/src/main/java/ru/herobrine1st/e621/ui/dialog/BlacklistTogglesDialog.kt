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

package ru.herobrine1st.e621.ui.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.placeholder
import com.google.accompanist.placeholder.material3.shimmer
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.BlacklistTogglesDialogComponent
import ru.herobrine1st.e621.preference.LocalPreferences
import kotlin.math.floor

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BlacklistTogglesDialog(
    component: BlacklistTogglesDialogComponent
) {
    val maxListHeight = floor(
        LocalConfiguration.current.screenHeightDp
            .times(0.4f) // Take 40% of screen height
            .div(49f)    // 49 dp is the height of one entry (48 dp checkbox/switch + 1 dp separator)
    )                          // Floor for all the perfectionists
        .times(49f)      // Return back
        .minus(1f)       // Remove 1 pixel because on my device that "last" separator from lazy layout is visible just for 1 pixel (or simply placebo)
        .dp

    var isBlacklistUpdating by remember { mutableStateOf(false) }
    val blacklistEntries by component.entriesFlow.collectAsState(initial = null)

    ActionDialog(
        title = stringResource(R.string.blacklist),
        actions = {
            if (isBlacklistUpdating) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            FilledTonalButton(
                onClick = component::onClose,
                enabled = !isBlacklistUpdating
            ) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = {
                    isBlacklistUpdating = true
                    component.applyChanges {
                        isBlacklistUpdating = false
                        component.onClose()
                    }
                },
                enabled = !isBlacklistUpdating
            ) {
                Text(stringResource(R.string.apply))
            }
        }, onDismissRequest = component::onClose
    ) {
        val isBlacklistEnabled = LocalPreferences.current.blacklistEnabled
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.toggleable(
                value = isBlacklistEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = component::toggleBlacklist
            )
        ) {
            AnimatedContent(
                isBlacklistEnabled,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideIntoContainer(AnimatedContentScope.SlideDirection.Right) +
                                fadeIn(spring(stiffness = Spring.StiffnessMedium)) with
                                slideOutOfContainer(AnimatedContentScope.SlideDirection.Right) +
                                fadeOut(spring(stiffness = Spring.StiffnessMedium))
                    } else {
                        slideIntoContainer(AnimatedContentScope.SlideDirection.Left) + fadeIn(
                            spring(
                                stiffness = Spring.StiffnessMedium
                            )
                        ) with
                                slideOutOfContainer(AnimatedContentScope.SlideDirection.Left) + fadeOut(
                            spring(stiffness = Spring.StiffnessMedium)
                        )
                    }.using(
                        // Disable clipping since the faded slide-in/out should
                        // be displayed out of bounds.
                        SizeTransform(clip = false)
                    )
                }
            ) {
                Text(
                    stringResource(if (it) R.string.blacklist_enabled else R.string.blacklist_disabled)
                )
            }
            Spacer(Modifier.weight(1f))
            Switch(
                checked = isBlacklistEnabled,
                onCheckedChange = component::toggleBlacklist
            )
        }

        Box(
            Modifier.heightIn(max = maxListHeight),
        ) {
            val targetState = when {
                blacklistEntries == null -> BlacklistTogglesDialogState.Loading
                blacklistEntries!!.isEmpty() -> BlacklistTogglesDialogState.Empty
                else -> BlacklistTogglesDialogState.Ready
            }

            AnimatedContent(
                targetState = targetState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) with
                            fadeOut(animationSpec = tween(90))
                }
            ) { state ->
                when (state) {
                    BlacklistTogglesDialogState.Loading -> Box(
                        Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Divider(Modifier.align(Alignment.TopCenter))
                        CircularProgressIndicator()
                        Divider(Modifier.align(Alignment.BottomCenter))
                    }

                    BlacklistTogglesDialogState.Empty -> Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.dialog_blacklist_empty))
                    }

                    BlacklistTogglesDialogState.Ready -> Column {
                        Divider()
                        LazyColumn {
                            val entries = blacklistEntries!!
                            item {
                                val isAllEnabled = entries.all { it.enabled }
                                BlacklistEntryLine(
                                    value = isAllEnabled,
                                    onValueChange = {
                                        entries.forEach {
                                            it.enabled = !isAllEnabled
                                        }
                                    },
                                    text = stringResource(R.string.selection_all),
                                    isBlacklistUpdating = isBlacklistUpdating
                                )
                                Divider()
                            }
                            itemsIndexed(entries) { i, entry ->
                                BlacklistEntryLine(
                                    value = entry.enabled,
                                    onValueChange = { entry.enabled = it },
                                    text = entry.query,
                                    isBlacklistUpdating = isBlacklistUpdating,
                                    showResetButton = entry.isChanged,
                                    onReset = { entry.resetChanges() }
                                )
                                if (i < entries.size - 1)
                                    Divider()
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun BlacklistEntryLine(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    text: String,
    isBlacklistUpdating: Boolean,
    showResetButton: Boolean = false,
    onReset: () -> Unit = {},
    placeholder: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.toggleable(
            value,
            remember { MutableInteractionSource() },
            null,
            onValueChange = onValueChange
        )
    ) {
        Text(
            text,
            modifier = Modifier
                .weight(1f)
                .placeholder(placeholder, highlight = PlaceholderHighlight.shimmer()),
            color = if (isBlacklistUpdating) Color.Gray else Color.Unspecified
        )
        if (showResetButton) {
            IconButton(
                enabled = !isBlacklistUpdating,
                onClick = onReset
            ) {
                Icon(
                    Icons.Outlined.Undo,
                    contentDescription = stringResource(R.string.cancel)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Checkbox(
            checked = value,
            onCheckedChange = onValueChange,
            enabled = !placeholder && !isBlacklistUpdating,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary),
        )
    }
}

private enum class BlacklistTogglesDialogState {
    Loading, Empty, Ready
}