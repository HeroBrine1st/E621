package ru.herobrine1st.e621.util

import androidx.compose.runtime.compositionLocalOf
import java.time.Clock

val LocalClock = compositionLocalOf<Clock> { error("No clock found") }