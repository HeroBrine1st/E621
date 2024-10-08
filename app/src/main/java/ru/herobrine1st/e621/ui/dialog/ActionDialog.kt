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

package ru.herobrine1st.e621.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp

// Clone of AlertDialog (that one with slots), but with more than 2 action buttons
// and dedicated to actions in its content field
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActionDialog(
    title: String,
    actions: @Composable (() -> Unit),
    onDismissRequest: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit)
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        // Fix size change
        // ComposeView uses first measured size as min and max constrains for next measurements
        // Ignore minimum constraints, allowing dialog to properly reduce its size
        Layout(
            content = {
                Surface(
                    shape = AlertDialogDefaults.shape,
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Fix dismiss on any click
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(all = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier
                                .align(Alignment.Start)
                        )
                        Spacer(Modifier.height(16.dp))
                        content()
                        Spacer(Modifier.height(24.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            actions()
                        }
                    }
                }
            },
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest
            )
        ) { measurables, constraints ->
            // This layout causes "Prior agent invocations in this VM" in logs
            // Many of them, about 20-30 maximum observed
            val constraintsWithoutMinimumHeight = constraints.copy(minHeight = 0)
            val placeables = measurables.map { it.measure(constraintsWithoutMinimumHeight) }
            val width = placeables.maxOf { it.width }
            val height = placeables.maxOf { it.height }.coerceAtLeast(constraints.minHeight)

            return@Layout layout(width, height) {
                // Align to center
                placeables.forEach {
                    it.place((width - it.width) / 2, (height - it.height) / 2)
                }
            }
        }

    }
}