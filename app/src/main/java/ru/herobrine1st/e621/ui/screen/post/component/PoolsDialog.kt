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

package ru.herobrine1st.e621.ui.screen.post.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.placeholder
import com.google.accompanist.placeholder.material3.shimmer
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.post.PoolsDialogComponent
import ru.herobrine1st.e621.ui.dialog.ActionDialog

@Composable
fun PoolsDialog(component: PoolsDialogComponent) {
    ActionDialog(
        title = stringResource(id = R.string.pools_dialog_select_pool),
        content = {
            component.pools.forEach {
                Row(
                    Modifier
                        .clickable(enabled = it != null) {
                            component.onClick(it ?: return@clickable)
                        }
                        .placeholder(
                            visible = it == null,
                            highlight = PlaceholderHighlight.shimmer(),
                        )
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Text(
                        // underscores are spaces, I guess
                        it?.name?.replace('_', ' ') ?: ""
                    )
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        actions = {
            FilledTonalButton(onClick = component::onDismiss) {
                Text(stringResource(R.string.dialog_dismiss))
            }
        },
        onDismissRequest = component::onDismiss,
    )
}