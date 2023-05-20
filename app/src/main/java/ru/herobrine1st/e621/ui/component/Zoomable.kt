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

import androidx.annotation.FloatRange
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
fun Zoomable(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    var translation by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            // TODO fling gesture (inertial pan, like on G Maps, it is very satisfying)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                    scale = (scale * gestureZoom).coerceAtLeast(1f)
                    // I'm not a talented *mathematician*, but it seems like
                    // I can prove this formula is right
                    translation = (
                            // Scale, using old centroid as origin
                            (translation - centroid) * gestureZoom +
                                    // Use new centroid as origin
                                    centroid + pan
                            )
                        .coerceInSize(size, scale)
                }
            }
            .graphicsLayer {
                translationX = translation.x
                translationY = translation.y
                scaleX = scale
                scaleY = scale
                // Formula is going to be complicated without this. With this origin is (0;0)
                // and easily manipulated without knowing size of container
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .onSizeChanged {
                size = it
                // FIXME if fillMax* modifier is applied, empty space is considered as zoomable
                // so that it participate in the function below as usable space
                // and so user can zoom enough and pan the content out of bounds
                // while this function is trying to prevent that
                // Possible solution: place content in its own box and move
                // onSizeChanged and graphicsLayer there
                // (and center that box in outer box)
                //
                // I gathered some user feedback and some say this is good feature.
                translation = translation.coerceInSize(size, scale)
            },
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints,
        content = content,
    )
}

// Assumes transformOrigin is (0;0)

@CheckReturnValue
private fun Offset.coerceInSize(size: IntSize, @FloatRange(from = 1.0) scale: Float): Offset {
    val constrainScalar = scale - 1f
    val constrainOffset = Offset(
        size.width * constrainScalar,
        size.height * constrainScalar
    )
    return Offset(
        // Inverted because negative values is to the right
        // and 0 is the leftmost
        x.coerceIn(-constrainOffset.x, 0f),
        y.coerceIn(-constrainOffset.y, 0f)
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
            model = "https://blog.jetbrains.com/wp-content/uploads/2023/02/DSGN-15525-Blog-Post-about-Kotlin-2.0_kotlinlang.org_.png",
            contentDescription = "Random image from my browser"
        )
    }
}