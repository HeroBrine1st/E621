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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun BoxWithConstraintsScope.GoingToFullscreenAnimation(
    isFullscreen: Boolean,
    contentAspectRatio: Float,
    getContentPositionRelativeToParent: () -> Offset,
    animationSpec: FiniteAnimationSpec<Float> = spring(stiffness = Spring.StiffnessMedium),
    assumeTranslatedComponentIsInFullscreenContainerCentered: Boolean = false,
    onExit: () -> Unit,
    // size change is not implemented
    componentBackground: @Composable () -> Unit, // Should fill max size
    translatedComponent: @Composable () -> Unit  // should not change its size if constraints are unchanged
) {
    val density = LocalDensity.current

    val animatable = remember {
        Animatable(
            initialValue = Float.NaN,
            visibilityThreshold = with(density) { 8.dp.toPx() }
        )
    }

    val compensation = remember {
        with(density) {
            if (assumeTranslatedComponentIsInFullscreenContainerCentered) ((maxWidth / contentAspectRatio - maxHeight) / 2).toPx()
            else 0f
        }
    }

    var isOpenedOrClosing by remember { mutableStateOf(false) }

    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            // FIXME flicker
            animatable.snapTo(getContentPositionRelativeToParent().y + compensation)
            animatable.animateTo(
                with(density) {
                    if (assumeTranslatedComponentIsInFullscreenContainerCentered) 0f
                    else ((maxHeight - maxWidth / contentAspectRatio) / 2).toPx()
                },
                animationSpec = animationSpec
            )
            isOpenedOrClosing = true
        } else if (!animatable.value.isNaN()) {
            animatable.animateTo(getContentPositionRelativeToParent().y + compensation)
            onExit()
            isOpenedOrClosing = false
        }
    }

    AnimatedVisibility(
        visible = isFullscreen,
        enter = fadeIn(animationSpec = animationSpec),
        exit = fadeOut(animationSpec = animationSpec),
    ) {
        componentBackground()
    }
    if (animatable.isRunning || isFullscreen || isOpenedOrClosing) Box(
        Modifier
            .absoluteOffset {
                val y =
                    if (animatable.isRunning || isOpenedOrClosing) animatable.value.toInt()
                    else (getContentPositionRelativeToParent().y + compensation).toInt() // Flicker fix
                IntOffset(0, y)
            }
            .align(Alignment.TopStart)
    ) {
        translatedComponent()
    }
}