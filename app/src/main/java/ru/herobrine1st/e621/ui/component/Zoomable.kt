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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

const val MAX_SCALE_DEFAULT = 5f

fun Modifier.zoomable(state: ZoomableState) = this
    .pointerInput(state) {
        detectTransformGestures { centroid, pan, gestureZoom, _ ->
            state.handleTransformationGesture(centroid, pan, gestureZoom)
        }
    }
    .graphicsLayer {
        translationX = state.translation.x
        translationY = state.translation.y
        scaleX = state.scale
        scaleY = state.scale
        // Formula is going to be complicated without this. With this origin is (0;0)
        // and easily manipulated without knowing size of container
        transformOrigin = TransformOrigin(0f, 0f)
    }
    // TODO add layout here to get size of underlying composable
    .onSizeChanged {
        state.onSizeChanged(it)
    }


class ZoomableState(
    @FloatRange(from = 1.0) val maxScale: Float = MAX_SCALE_DEFAULT,
    initialScale: Float = 1f,
    initialTranslation: Offset = Offset.Zero
) {

    init {
        require(initialScale >= 1f) { "Initial scale should be equal or more than 1, got $initialScale" }
        // It's the caller responsibility to check initialTranslation. Check above is here only because application will anyway crash.
    }

    // TODO saveable
    // TODO animation (use targetValue in Saver and add constructor parameters for initial values)
    // This class assumes that transformOrigin is (0;0)
    var translation by mutableStateOf(initialTranslation)
        private set
    var scale by mutableStateOf(initialScale)
        private set
    var size by mutableStateOf(IntSize.Zero)
        private set

    fun handleTransformationGesture(centroid: Offset, pan: Offset, gestureScale: Float) {
        // TODO fling gesture (inertial pan, like on G Maps, it is very satisfying)
        // I've seen VelocityTracker or something like that in other implementations
        // Animatable's snapTo can be used for gesture and animateDecay for inertia
        val oldScale = scale
        scale = (scale * gestureScale).coerceIn(minimumValue = 1f, maximumValue = maxScale)
        // upper bound is causing floating to lower right corner when user tries to zoom beyond the limit.
        val consumedGestureScale = if (scale != maxScale) gestureScale else scale / oldScale
        // I'm not a talented *mathematician*, but it seems like I can prove this formula is right
        translation = (
                // Scale, using old centroid as origin
                (translation - centroid) * consumedGestureScale +
                        // Use new centroid as origin
                        centroid + pan
                )
            .coerceInSize()
    }

    fun onSizeChanged(intSize: IntSize) {
        size = intSize
        translation = translation.coerceInSize()
    }

    @CheckReturnValue
    private fun Offset.coerceInSize(): Offset {
        // FIXME if fillMax* modifier is applied, empty space is considered as zoomable
        // so that it participate in the function below as usable space
        // and so user can zoom enough and pan the content out of bounds
        // while this function is trying to prevent that
        // Possible solution: add aspectRatio or size of content parameters
        //
        // I gathered some user feedback and some say this is good feature.

        // Inverted because negative values is to the right
        // and 0 is the leftmost
        val constrainScalar = 1f - scale
        val xConstrain = size.width * constrainScalar
        val yConstrain = size.height * constrainScalar
        return Offset(
            x.coerceIn(xConstrain, 0f),
            y.coerceIn(yConstrain, 0f)
        )
    }
}

@Composable
fun rememberZoomableState(
    @FloatRange(from = 1.0) maxScale: Float = MAX_SCALE_DEFAULT,
    initialScale: Float = 1f,
    initialTranslation: Offset = Offset.Zero
) =
    remember { ZoomableState(maxScale, initialScale, initialTranslation) }


@Preview
@Composable
fun ZoomablePreview() {
    AsyncImage(
        // This image is random-number-th image I found in my browser
        // Guaranteed to be random
        // https://xkcd.com/221/
        model = "https://blog.jetbrains.com/wp-content/uploads/2023/02/DSGN-15525-Blog-Post-about-Kotlin-2.0_kotlinlang.org_.png",
        contentDescription = "Random image from my browser",
        modifier = Modifier
            .zoomable(rememberZoomableState())
            .fillMaxHeight()

    )
}