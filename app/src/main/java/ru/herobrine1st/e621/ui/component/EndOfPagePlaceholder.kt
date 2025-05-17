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

package ru.herobrine1st.e621.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.paging.api.LoadState

// edge of page, start and end of page or anything, it just doesn't matter while the name is clear
fun LazyListScope.endOfPagePlaceholder(loadState: LoadState, onRetry: () -> Unit) {
    when (loadState) {
        is LoadState.Loading -> {
            item {
                Base {
                    Spacer(modifier = Modifier.height(4.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        is LoadState.Error -> {
            item {
                Base {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.unknown_error))
                    Button(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        else -> {}
    }
}

fun LazyStaggeredGridScope.endOfPagePlaceholder(loadState: LoadState, onRetry: () -> Unit) {
    when (loadState) {
        is LoadState.Loading -> {
            item(span = StaggeredGridItemSpan.FullLine) {
                Base {
                    Spacer(modifier = Modifier.height(4.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        is LoadState.Error -> {
            item(span = StaggeredGridItemSpan.FullLine) {
                Base {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.unknown_error))
                    Button(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        else -> {}
    }
}