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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.abs


const val MAX_SCALE_DEFAULT = 5f

fun Modifier.zoomable(state: ZoomableState) = this
    .pointerInput(state) {
        detectTransformGestures(
            onGestureStart = state::handleGestureStart,
            onGesture = { centroid, pan, gestureZoom, uptimeMillis ->
                state.handleTransformationGesture(
                    centroid,
                    pan,
                    gestureZoom,
                    uptimeMillis
                )
            },
            onGestureEnd = state::handleGestureEnd,
        )
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

private suspend inline fun PointerInputScope.detectTransformGestures(
    crossinline onGestureStart: () -> Unit = {},
    crossinline onGesture: (centroid: Offset, pan: Offset, zoom: Float, uptimeMillis: Long) -> Unit,
    crossinline onGestureEnd: () -> Unit = {}
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        awaitFirstDown(requireUnconsumed = false)
        onGestureStart()
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    if (zoomChange != 1f || panChange != Offset.Zero)
                        onGesture(
                            centroid,
                            panChange,
                            zoomChange,
                            event.changes.fastFirst { it.positionChanged() }.uptimeMillis
                        )
                    event.changes.fastForEach {
                        if (it.positionChanged()) it.consume()
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
        onGestureEnd()
    }
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
private inline fun <T> List<T>.fastFirst(predicate: (T) -> Boolean): T {
    contract { callsInPlace(predicate) }
    fastForEach { if (predicate(it)) return it }
    throw NoSuchElementException("Collection contains no element matching the predicate.")
}

class ZoomableState(
    @FloatRange(from = 1.0) val maxScale: Float = MAX_SCALE_DEFAULT,
    initialScale: Float = 1f,
    initialTranslation: Offset = Offset.Zero
) : RememberObserver {
    // TODO saveable
    // This class assumes that transformOrigin is (0;0)

    init {
        require(initialScale >= 1f) { "Initial scale should be equal or more than 1, got $initialScale" }
        // It's the caller responsibility to check initialTranslation. Check above is here only because application will anyway crash.
    }

    private val translationAnimatable =
        Animatable(initialTranslation, typeConverter = Offset.VectorConverter)
    private val scaleAnimatable = Animatable(initialScale).apply { updateBounds(1f, maxScale) }
    private val panVelocityTracker = VelocityTrackerDifferential()
    private val coroutineScope = CoroutineScope(AndroidUiDispatcher.Main)

    val translation by translationAnimatable.asState()
    val scale by scaleAnimatable.asState()

    var size by mutableStateOf(IntSize.Zero)
        private set

    // Dirty workaround to ignore scale gestures in pan animation
    // TODO if user exceeds touch slop with one finger (regardless of what was done before)
    //      and then releases, animate as well
    private var scaleGesturePerformed = false

    fun handleGestureStart() {
        scaleGesturePerformed = false
        panVelocityTracker.resetTracking()
    }

    fun handleTransformationGesture(
        centroid: Offset,
        pan: Offset,
        gestureScale: Float,
        timeMs: Long
    ) {
        val oldScale = scale
        val newScale =
            (oldScale * gestureScale).coerceIn(
                minimumValue = scaleAnimatable.lowerBound,
                maximumValue = scaleAnimatable.upperBound
            )

        // upper bound is causing floating to lower right corner when user tries to zoom beyond the limit.
        val consumedGestureScale = if (newScale != maxScale) gestureScale else newScale / oldScale
        // I'm not a talented *mathematician*, but it seems like I can prove this formula is right
        val newTranslation =
            // Scale, using old centroid as origin
            (translation - centroid) * consumedGestureScale +
                    // Use new centroid as origin
                    centroid + pan

        panVelocityTracker.addVector(timeMs, pan)

        coroutineScope.launch {
            scaleAnimatable.snapTo(newScale)
            setTranslationBounds()
            translationAnimatable.snapTo(newTranslation)
        }
        scaleGesturePerformed = scaleGesturePerformed || gestureScale != 1f
    }

    fun handleGestureEnd() {
        if (scaleGesturePerformed) {
            scaleGesturePerformed = false
            return
        }
        val lowerBound = translationAnimatable.lowerBound!!
        coroutineScope.launch {
            // splineBasedDecay is magnifying vectors to one of the axes
            // exponentialDecay doesn't do that and still feels good
            val result = translationAnimatable.animateDecay(
                panVelocityTracker.calculateVelocityAsOffset(),
                exponentialDecay()
            )
            if (result.endReason == AnimationEndReason.BoundReached) {
                val velocity =
                    result.endState.typeConverter.convertFromVector(result.endState.velocityVector)
                val endValue = result.endState.value
                if (endValue.x == 0f || endValue.x == lowerBound.x) {
                    translationAnimatable.animateDecay(velocity.copy(x = 0f), exponentialDecay())
                } else {
                    translationAnimatable.animateDecay(velocity.copy(y = 0f), exponentialDecay())
                }
            }
        }
    }

    fun onSizeChanged(intSize: IntSize) {
        size = intSize
        setTranslationBounds()
    }

    private fun setTranslationBounds() {
        // FIXME if fillMax* modifier is applied, empty space is considered as zoomable
        // so that it participate in the function below as usable space
        // and so user can zoom enough and pan the content out of bounds
        // while this function is trying to prevent that
        // Possible solution: add aspectRatio or size of content parameters, or use Modifier.layout
        // to get it
        //
        // I gathered some user feedback and some say this is good feature.


        // Inverted because negative values is to the right
        // and 0 is the leftmost
        val constrainScalar = 1f - scale
        val xConstrain = size.width * constrainScalar
        val yConstrain = size.height * constrainScalar
        translationAnimatable.updateBounds(Offset(xConstrain, yConstrain), Offset.Zero)
    }

    override fun onRemembered() {
        // Nothing to do
        // Maybe check that it is remembered only once?
    }

    override fun onForgotten() {
        coroutineScope.cancel()
    }

    override fun onAbandoned() {
        coroutineScope.cancel()
    }
}

// Differential because centroid can jump to other finger after releasing one
// Ignoring scale gestures will not help: pretty skilled user can pan with two fingers at constant distance between them
class VelocityTrackerDifferential {
    private val xVelocityTracker = VelocityTracker1D(isDataDifferential = true)
    private val yVelocityTracker = VelocityTracker1D(isDataDifferential = true)

    fun addVector(timeMillis: Long, vector: Offset) {
        xVelocityTracker.addDataPoint(timeMillis, vector.x)
        yVelocityTracker.addDataPoint(timeMillis, vector.y)
    }

    fun calculateVelocityAsOffset(): Offset {
        return Offset(xVelocityTracker.calculateVelocity(), yVelocityTracker.calculateVelocity())
    }

    fun resetTracking() {
        xVelocityTracker.resetTracking()
        yVelocityTracker.resetTracking()
    }
}


@Composable
fun rememberZoomableState(
    @FloatRange(from = 1.0) maxScale: Float = MAX_SCALE_DEFAULT,
    initialScale: Float = 1f,
    initialTranslation: Offset = Offset.Zero
) = remember { ZoomableState(maxScale, initialScale, initialTranslation) }


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