/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.util

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import ru.herobrine1st.e621.BuildConfig
import kotlin.math.pow


val USER_AGENT = BuildConfig.USER_AGENT_TEMPLATE.format(Build.VERSION.RELEASE, BuildConfig.BUILD_TYPE)

@Composable
inline fun <T> time(name: String, block: @Composable () -> T): T {
    val start = System.nanoTime()
    val res = block()
    Log.d("Timer-Own", "$name taken ${(System.nanoTime() - start) / (10.0.pow(9))} s")
    return res
}

fun Activity.restart() {
    val intent = intent
    finish()
    startActivity(intent)
}