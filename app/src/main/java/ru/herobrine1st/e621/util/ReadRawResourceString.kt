package ru.herobrine1st.e621.util

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.nio.charset.Charset

@Composable
@ReadOnlyComposable
fun readRawResourceString(@RawRes resId: Int): String {
    LocalConfiguration.current
    return LocalContext.current.resources.openRawResource(resId).reader(Charset.forName("UTF-8")).use { it.readText() }
}