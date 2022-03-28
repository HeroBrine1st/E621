package ru.herobrine1st.e621.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import kotlin.math.roundToInt

@Composable
fun getScreenSize(): Pair<Int, Int> {
    LocalConfiguration.current.let {
        val magic = it.densityDpi / 160f
        return (it.screenWidthDp.toFloat() * magic).roundToInt() to (it.screenHeightDp * magic).roundToInt()
    }
}