package ru.herobrine1st.e621.util

import android.os.Build
import ru.herobrine1st.e621.BuildConfig

val USER_AGENT = BuildConfig.USER_AGENT_TEMPLATE.format(Build.VERSION.RELEASE, BuildConfig.BUILD_TYPE)