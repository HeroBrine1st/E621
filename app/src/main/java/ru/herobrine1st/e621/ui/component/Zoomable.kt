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

package ru.herobrine1st.e621.ui.component

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import javax.annotation.CheckReturnValue

/**
 * A layout composable that can be zoomed. It supports dragging zoomed image (with one and two fingers)
 * and zooming in and out (only to initial size).
 */
@Composable
fun Zoomable(content: @Composable BoxScope.() -> Unit) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                // Warning! Math ahead!
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    // Just to avoid saving old scale. Those values will be in any way saved to stack, but later.
                    val centroidInAbsoluteScale = centroid / scale
                    val panInAbsoluteScale = pan / scale

                    // Lower bound of size, so that it can't be smaller than Zoomable itself
                    scale = (scale * gestureZoom).coerceAtLeast(1f)

                    // Terrible math taken from reference
                    // Rotation is not needed I think. You can just rotate your device
                    // (if you use an emulator, just rotate your 27'' display)
                    // (buy longer and more durable video cable if needed)
                    // (also consider switching to VGA or DVI, as those are harder to eject)
                    // Be aware that now [scale] is the actual scale, in response to gesture
                    offset = ((offset + centroidInAbsoluteScale) -
                            (centroid / scale + panInAbsoluteScale))
                        .coercePanWithinSize(size, scale)

                }
            }
            .graphicsLayer {
                translationX = -offset.x * scale
                translationY = -offset.y * scale
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .onSizeChanged {
                size = it
                offset = offset.coercePanWithinSize(size, scale)
            },
        content = content,
    )
}

// Assumes transformOrigin is (0;0) and offset is non-scaled, so it is private
@CheckReturnValue
private fun Offset.coercePanWithinSize(size: IntSize, scale: Float): Offset {
    // Think of it as (scale - 1f) / scale (as offset is multiplied by scale later)
    // This makes sense if you imagine Zoomable as a window from which you see the [content]
    // So that you don't want to pan more than zoomed size of image (which is size of Zoomable times scale)
    // minus actual size of Zoomable
    val constrainScalar = 1 - 1f / scale
    val constrainOffset = Offset(
        size.width * constrainScalar,
        size.height * constrainScalar
    )
    return Offset(
        x.coerceIn(0f, constrainOffset.x),
        y.coerceIn(0f, constrainOffset.y)
    )
}


@Preview
@Composable
fun ZoomablePreview() {
    Zoomable {
        AsyncImage(
            // This image is random-number-th image I found in my browser
            // Guaranteed to be random
            // https://xkcd.com/221/
            model = "https://kotlinlang.org/assets/images/index/banners/kotlin_2.0_blog.jpg",
            contentDescription = "Random image from my browser"
        )
    }
}