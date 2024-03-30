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

package ru.herobrine1st.e621.ui.screen.post.component

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.posts.PostListingComponent
import ru.herobrine1st.e621.ui.component.RenderBB

@Composable
fun PoolInfoCard(info: PostListingComponent.InfoState.PoolInfo, modifier: Modifier = Modifier) {
    val pool = info.pool
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(pool.normalizedName, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.pool_created_relative_date,
                    DateUtils.getRelativeTimeSpanString(
                        pool.createdAt.toEpochMilliseconds(),
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS
                    )
                )
            )
            if (pool.updatedAt != null) Text(
                stringResource(
                    R.string.pool_updated_relative_date,
                    DateUtils.getRelativeTimeSpanString(
                        pool.updatedAt.toEpochMilliseconds(),
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS
                    )
                )
            )
            if (pool.isActive) Text(stringResource(R.string.pool_is_active))
            else Text(stringResource(R.string.pool_is_not_active))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.description),
                style = MaterialTheme.typography.headlineSmall
            )
            if (pool.description.isNotBlank()) SelectionContainer {
                RenderBB(info.description)
            } else Text(stringResource(R.string.empty_description))
        }
    }
}