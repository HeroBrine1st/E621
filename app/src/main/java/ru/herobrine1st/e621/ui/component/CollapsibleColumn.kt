/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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


import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.util.LimitHeightShape

/**
 * Make [content] collapsible if its height exceeds [collapsedHeight].
 *
 * This layout behaves much like simple [Column] except it has two slots for content and button.
 *
 * **Content is measured eagerly!** It means that it will compose all content, even if some
 * elements are not visible.
 *
 * @param collapsedHeight minimal height (exclusive) which collapse/expand functionality will work from
 * @param state state of this layout
 * @param button collapse/expand button at the bottom of this layout. Composed lazily - this
 * parameter is unused if height of [content] if less that [collapsedHeight].
 */
// TODO gradient
@Composable
fun CollapsibleColumn(
    collapsedHeight: Dp,
    state: CollapsibleColumnState = rememberCollapsibleColumnState(),
    button: @Composable (expanded: Boolean, onClick: () -> Unit) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val collapsedHeightPx = with(density) { collapsedHeight.roundToPx() }
    val heightAnimatable = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    SubcomposeLayout { constraints ->
        val contentMeasured = subcompose(CollapsibleColumnSlot.CONTENT, content).map {
            it.measure(constraints)
        }
        val contentHeight = contentMeasured.sumOf { it.height }
        val buttonMeasured = if (contentMeasured.sumOf { it.height } > collapsedHeightPx) {
            subcompose(CollapsibleColumnSlot.BUTTON) {
                button(state.expanded) {
                    state.expanded = !state.expanded
                }
            }.map { it.measure(constraints) }
        } else emptyList()

        val totalHeight = heightAnimatable.value.toInt() + buttonMeasured.sumOf { it.height }
        if (heightAnimatable.lowerBound == null) {
            heightAnimatable.updateBounds(
                contentHeight.coerceAtMost(collapsedHeightPx).toFloat(),
                contentHeight.toFloat()
            )
        }
        coroutineScope.launch {
            heightAnimatable.animateTo((if(state.expanded) contentHeight else contentHeight.coerceAtMost(collapsedHeightPx)).toFloat())
        }

        layout(constraints.maxWidth, totalHeight) {
            var cumulativeHeight = 0
            val currentHeight = heightAnimatable.value.toInt()
            for (placeable in contentMeasured) {
                if (cumulativeHeight + placeable.height < currentHeight) {
                    placeable.placeRelative(0, cumulativeHeight)
                    cumulativeHeight += placeable.height
                } else {
                    placeable.placeRelativeWithLayer(0, cumulativeHeight) {
                        shape = LimitHeightShape((currentHeight - cumulativeHeight).toFloat())
                        clip = true
                    }
                    cumulativeHeight = currentHeight
                    break
                }
            }
            for (placeable in buttonMeasured) {
                placeable.placeRelative(0, cumulativeHeight)
                cumulativeHeight += placeable.height
            }
        }
    }
}

@Composable
fun rememberCollapsibleColumnState(expandedInitial: Boolean = false) = remember {
    CollapsibleColumnState(expandedInitial)
}

@Preview
@Composable
private fun Preview() {
    CollapsibleColumn(collapsedHeight = 16.dp, button = { expanded, onClick ->
        Button(onClick = onClick) {
            Text("Toggle $expanded")
        }
    }) {
        Text("123\n".repeat(16))
    }
}

class CollapsibleColumnState(
    expandedInitial: Boolean = false
) {
    var expanded by mutableStateOf(expandedInitial)
}

enum class CollapsibleColumnSlot {
    CONTENT,
    BUTTON;
}