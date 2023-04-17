package ru.herobrine1st.e621.ui.animation

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimator

fun reducedSlide(slideFactor: Float) = stackAnimator { factor, _, content ->
    content(
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(
                        x = (placeable.width.toFloat() * factor * slideFactor).toInt(),
                        y = 0
                    )
                }
            }
    )
}