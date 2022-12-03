package ru.herobrine1st.e621.util

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import okhttp3.OkHttp
import ru.herobrine1st.e621.BuildConfig
import kotlin.math.roundToInt

val USER_AGENT = BuildConfig.USER_AGENT_TEMPLATE.format(Build.VERSION.RELEASE, OkHttp.VERSION)