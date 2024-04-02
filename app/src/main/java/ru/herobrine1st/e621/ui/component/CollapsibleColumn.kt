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


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val MS_PER_SCREEN = 500
private const val MINIMUM_MS_FOR_ANIMATION = 250

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
    val collapsedHeightPx = with(density) { collapsedHeight.toPx() }

    SubcomposeLayout { constraints ->
        val contentMeasured = subcompose(CollapsibleColumnSlot.CONTENT, content).map {
            it.measure(constraints)
        }

        val contentHeightPx = contentMeasured.sumOf { it.height }.toFloat()
        val collapsedContentHeightPx = contentHeightPx.coerceAtMost(collapsedHeightPx)
        state.updateContentHeight(collapsedContentHeightPx, contentHeightPx)

        // Effectively `contentHeightPx > collapsedHeightPx`, but slightly faster (i think)
        val buttonMeasured = if (contentHeightPx != collapsedContentHeightPx) {
            subcompose(CollapsibleColumnSlot.BUTTON) {
                button(state.expanded) {
                    state.expanded = !state.expanded
                }
            }.map { it.measure(constraints) }
        } else emptyList()

        val totalHeight = state.currentHeight.toInt() + buttonMeasured.sumOf { it.height }
        layout(constraints.maxWidth, totalHeight) {
            var cumulativeHeight = 0
            val currentHeight = state.currentHeight
            for (placeable in contentMeasured) {
                if (cumulativeHeight + placeable.height < currentHeight) {
                    placeable.placeRelative(0, cumulativeHeight)
                    cumulativeHeight += placeable.height
                } else {
                    placeable.placeRelativeWithLayer(0, cumulativeHeight) {
                        shape = LimitHeightShape(currentHeight - cumulativeHeight)
                        clip = true
                    }
                    cumulativeHeight = currentHeight.toInt()
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
fun rememberCollapsibleColumnState(expandedInitial: Boolean = false): CollapsibleColumnState {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val coroutineScope = rememberCoroutineScope()
    val state = remember {
        CollapsibleColumnState(expandedInitial, coroutineScope)
    }

    LaunchedEffect(configuration.screenHeightDp, density) {
        with(density) {
            state.setDeviceHeightPx(configuration.screenHeightDp.dp.toPx())
        }
    }

    return state
}

@Preview
@Composable
private fun Preview() {
    CollapsibleColumn(collapsedHeight = 16.dp, button = { expanded, onClick ->
        Button(onClick = onClick) {
            Text("Toggle $expanded")
        }
    }, state = rememberCollapsibleColumnState(true)) {
        Text("123\n".repeat(16))
    }
}


class CollapsibleColumnState(
    expandedInitial: Boolean = false,
    private val coroutineScope: CoroutineScope
) {
    private val animatable = Animatable(if (expandedInitial) Float.POSITIVE_INFINITY else 0f)
    private var deviceHeightPx = 0f

    private var _expanded by mutableStateOf(expandedInitial)

    var expanded
        get() = _expanded
        set(v) {
            _expanded = v
            handleChange()
        }

    var animationSpec = tween<Float>()
        private set

    private fun handleChange() = coroutineScope.launch {
        val value = if (expanded) animatable.upperBound!! else animatable.lowerBound!!
        animatable.animateTo(
            targetValue = value,
            animationSpec = animationSpec
        )
    }

    internal fun setDeviceHeightPx(height: Float) {
        deviceHeightPx = height
        if (animatable.lowerBound != null) updateAnimation() // Race condition
    }

    internal fun updateContentHeight(collapsedHeight: Float, expandedHeight: Float) {
        if (animatable.lowerBound != collapsedHeight || animatable.upperBound != expandedHeight) {
            animatable.updateBounds(collapsedHeight, expandedHeight)
            if (deviceHeightPx != 0f) updateAnimation() // Race condition
            coroutineScope.launch {
                animatable.snapTo(if (expanded) expandedHeight else collapsedHeight)
            }
        }
    }

    private fun updateAnimation() {
        animationSpec = tween(
            durationMillis = ((animatable.upperBound!! - animatable.lowerBound!!) * MS_PER_SCREEN / deviceHeightPx)
                .roundToInt()
                .coerceAtLeast(MINIMUM_MS_FOR_ANIMATION)
        )
    }

    internal val currentHeight get() = animatable.value
}

enum class CollapsibleColumnSlot {
    CONTENT,
    BUTTON;
}

private class LimitHeightShape(private val height: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Rectangle(
        Rect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = height
        )
    )
}