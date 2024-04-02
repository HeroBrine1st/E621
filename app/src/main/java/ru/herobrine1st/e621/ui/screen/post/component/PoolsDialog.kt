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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.post.PoolsComponent
import ru.herobrine1st.e621.navigation.component.post.PoolsComponent.PoolState
import ru.herobrine1st.e621.ui.component.placeholder.PlaceholderHighlight
import ru.herobrine1st.e621.ui.component.placeholder.material3.placeholder
import ru.herobrine1st.e621.ui.component.placeholder.material3.shimmer
import ru.herobrine1st.e621.ui.dialog.ActionDialog

@Composable
fun PoolsDialog(component: PoolsComponent) {
    if (!component.showPools) return

    ActionDialog(
        title = stringResource(id = R.string.pools_dialog_select_pool),
        content = {
            component.pools.forEach { state ->
                Row(
                    Modifier
                        .clickable(enabled = state is PoolState.Successful) {
                            if (state is PoolState.Successful)
                                component.onClick(state.pool)
                        }
                        .placeholder(
                            visible = state is PoolState.NotLoaded,
                            highlight = PlaceholderHighlight.shimmer(),
                        )
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    when(state) {
                        is PoolState.Successful -> {
                            // TODO move to custom KSerializer
                            // hint: custom serializer for only one field is possible
                            Text(state.pool.name.replace('_', ' '))
                        }
                        is PoolState.Error -> {
                            Text(stringResource(R.string.unknown_error))
                        }
                        else -> Text("")
                    }
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