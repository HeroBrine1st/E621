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

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
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
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


const val MAX_SCALE_DEFAULT = 5f
const val TAG = "Zoomable"

fun Modifier.zoomable(state: ZoomableState, onTap: (position: Offset) -> Unit = {}) = this
    .pointerInput(state) {
        detectTransformGestures(
            onGestureStart = state::handleGestureStart,
            onGesture = state::handleTransformationGesture,
            onGestureEnd = state::handleGestureEnd,
            onDoubleTap = state::onDoubleTap,
            onSingleTap = onTap
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
    crossinline onGestureEnd: () -> Unit = {},
    crossinline onDoubleTap: (position: Offset) -> Unit = {},
    crossinline onSingleTap: (position: Offset) -> Unit = {},
) {
    awaitEachGesture {
        var cumZoom = 1f
        var cumPan = Offset.Zero
        var firstGestureIsTransformation = false
        val first = awaitFirstDown()
        onGestureStart()

        forEachEventUntilReleased { event ->
            val zoomChange = event.calculateZoom()
            val panChange = event.calculatePan()

            if (!firstGestureIsTransformation) {
                cumZoom *= zoomChange
                cumPan += panChange

                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1 - cumZoom) * centroidSize
                val panMotion = cumPan.getDistance()

                if (zoomMotion > viewConfiguration.touchSlop || panMotion > viewConfiguration.touchSlop) {
                    firstGestureIsTransformation = true
                } else return@forEachEventUntilReleased
            }
            // uncomment in case of strange bugs, also remove "return" above
            //if (pastTouchSlop) {
            if (zoomChange != 1f || panChange != Offset.Zero) {
                val centroid = event.calculateCentroid(useCurrent = false)
                onGesture(
                    centroid,
                    panChange,
                    zoomChange,
                    event.uptimeMillis
                )
            }
            event.changes.fastForEach {
                if (it.positionChanged()) it.consume()
            }
            //}
        }

        run {
            if (!firstGestureIsTransformation) {
                val second =
                    this@awaitEachGesture.withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                        var change: PointerInputChange
                        do {
                            change = awaitFirstDown()
                            if (change.uptimeMillis - first.uptimeMillis >= viewConfiguration.doubleTapMinTimeMillis)
                                break
                            // It does cancel this coroutine after timeout, but just in case
                        } while (change.uptimeMillis <= first.uptimeMillis + viewConfiguration.doubleTapTimeoutMillis)
                        change
                    }
                if (second == null) {
                    onSingleTap(first.position)
                    return@run
                }

                val centroid = second.position
                var secondGestureIsScaling = false
                cumPan = Offset.Zero
                var lastUptimeMillis: Long = 0

                if (centroid.isUnspecified) return@run // silently "return and run" (:D) if that ever occurs

                this@awaitEachGesture.forEachEventUntilReleased { event ->
                    lastUptimeMillis = event.uptimeMillis
                    val pan = event.calculatePan()

                    if (!secondGestureIsScaling) {
                        cumPan += pan
                        if (cumPan.getDistance() > viewConfiguration.touchSlop) {
                            secondGestureIsScaling = true
                        } else return@forEachEventUntilReleased

                    }
                    // TODO use DPI to make this behavior similar on every phone
                    //      but investigate whether it is required first.
                    //      it is probably not possible to do a "objects under the finger follow the finger", but I didn't investigate it.
                    // pow is used because a^x * a^y = a^(x+y), where a is constant.
                    val scaleCoefficient = 1.005f.pow(pan.y)

                    onGesture(centroid, Offset.Zero, scaleCoefficient, event.uptimeMillis)

                    event.changes.fastForEach {
                        if (it.positionChanged()) it.consume()
                    }
                }

                if (!secondGestureIsScaling && lastUptimeMillis - second.uptimeMillis < viewConfiguration.longPressTimeoutMillis) {
                    onDoubleTap(centroid)
                }
            }
        }

        onGestureEnd()

    }
}

private suspend inline fun AwaitPointerEventScope.forEachEventUntilReleased(block: (PointerEvent) -> Unit) {
    do {
        val event = awaitPointerEvent()
        if (event.changes.fastAny { it.isConsumed })
            break
        block(event)
    } while (event.changes.fastAny { it.pressed })
}

val PointerEvent.uptimeMillis get() = this.changes[0].uptimeMillis

class ZoomableState(
    @FloatRange(from = 1.0) maxScale: Float = MAX_SCALE_DEFAULT,
    initialScale: Float = 1f,
    initialTranslation: Offset = Offset.Zero,
    private val zoomSteps: List<Float> = listOf(
        1f,
        sqrt(maxScale), // half the max zoom
        // maxScale
    )
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

    private fun calculateTransformation(
        centroid: Offset,
        pan: Offset,
        scaleCoefficient: Float,
    ): Pair<Offset, Float> {
        val scale0 = scale
        val translation0 = translation

        val scale1 =
            (scale0 * scaleCoefficient).coerceIn(
                minimumValue = scaleAnimatable.lowerBound,
                maximumValue = scaleAnimatable.upperBound
            )

        val consumedScaleCoefficient =
            if (scale1 != scaleAnimatable.upperBound) scaleCoefficient else scale1 / scale0

        val translation1 = // Scale, using old centroid as origin
            (translation0 - centroid) * consumedScaleCoefficient +
                    centroid + pan // Use new centroid as origin

        return translation1 to scale1
    }

    fun handleTransformationGesture(
        centroid: Offset,
        pan: Offset,
        scaleCoefficient: Float,
        timeMs: Long
    ) {
        val (newTranslation, newScale) = calculateTransformation(
            centroid,
            pan,
            scaleCoefficient
        )

        panVelocityTracker.addVector(timeMs, pan)

        coroutineScope.launch {
            scaleAnimatable.snapTo(newScale)
            setTranslationBounds()
            translationAnimatable.snapTo(newTranslation)
        }
        scaleGesturePerformed = scaleGesturePerformed || scaleCoefficient != 1f
    }

    fun handleGestureEnd() {
        if (scaleGesturePerformed) {
            scaleGesturePerformed = false
            return
        }
        val lowerBound = translationAnimatable.lowerBound!!
        coroutineScope.launch {
            // splineBasedDecay magnifies vectors to one of the axes
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

    fun onDoubleTap(position: Offset) {
        // This algorithm simulates user performing manual scale gesture, reusing the code written for that.

        // It probably can be optimized, i.e. we can drop normalization of the scale by dropping multiplication
        //     (or making it optional, or copying code - whatever) in calculateTransformation,
        //     but it is not worth it, I think.

        // Here, normalization is division of scale by initialScale. This algorithm requires that
        //     scale initially is equal to 1f, and that requirement is fulfilled by normalization.

        // Also, I guess, there can be some mathematical "model" for this animation, like I did once
        //     to create exact formula for transformation (which turned out to be extremely simple,
        //     in contrast with the formula from google's example for transformation gestures).
        // If so, it then probably can be optimized further.

        // Code cleanup is probably not possible or requires duplicating code. I already tried to
        //     calculate initial value of [animate] by using log(initialScale, base = resultingScale).
        //     It eliminates the need for normalization, but then it is mathematically
        //     not possible to scale back to default (1f), probably requiring
        //     another normalization.. you got it.
        // One day I'll read this all with a clean head and clean it up..
        //
        // Now it just works.

        val initialScale = scale
        coroutineScope.launch {
            // Take next step, go to first if nothing found
            val stepIndex = zoomSteps.indexOfFirst { it > initialScale }.let {
                if (it == -1) 0
                else it % zoomSteps.size
            }
            val resultingScale = zoomSteps[stepIndex]
            // Normalize: this algorithm requires that scale is 1f initially
            val cumulativeScaleRequired = resultingScale / initialScale

            animate(0f, 1f, animationSpec = tween()) { parameter, _ ->
                val totalScale = cumulativeScaleRequired.pow(parameter)
                // The same as `totalScale / (scale / initialScale)`. `scale / initialScale` is normalization too.
                // Calculating "amount of relative zoom", which will be then multiplied back
                //     and divided forth by [calculateTransformation]. That's where optimization is possible.
                val scale = totalScale * initialScale / scale

                // Reused code for human gestures follows
                val (newTranslation, newScale) = calculateTransformation(
                    centroid = position,
                    pan = Offset.Zero,
                    scaleCoefficient = scale
                )

                this@launch.launch {
                    scaleAnimatable.snapTo(newScale)
                    setTranslationBounds()
                    translationAnimatable.snapTo(newTranslation)
                }
            }
        }
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
        return Offset(
            xVelocityTracker.calculateVelocity(),
            yVelocityTracker.calculateVelocity()
        )
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
    initialTranslation: Offset = Offset.Zero,
    zoomSteps: List<Float> = listOf(
        1f,
        sqrt(maxScale)
    )
) = remember { ZoomableState(maxScale, initialScale, initialTranslation, zoomSteps) }


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