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

package ru.herobrine1st.e621.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is an example startup benchmark.
 *
 * It navigates to the device's home screen, and launches the default activity.
 *
 * Before running this benchmark:
 * 1) switch your app's active build variant in the Studio (affects Studio runs only)
 * 2) add `<profileable android:shell="true" />` to your app's manifest, within the `<application>` tag
 *
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleStartupBenchmark {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

//    @Test
//    fun startup() = benchmarkRule.measureRepeated(
//        packageName = "ru.herobrine1st.e621",
//        metrics = listOf(StartupTimingMetric()),
//        iterations = 5,
//        startupMode = StartupMode.COLD
//    ) {
//        pressHome()
//        startActivityAndWait()
//    }

    @Test
    fun startup() = baselineProfileRule.collectBaselineProfile(
        packageName = "ru.herobrine1st.e621",
        profileBlock = {
            pressHome()
            startActivityAndWait()
            device.findObject(By.text("I understand"))?.click()
            device.findObject(By.text("I understand"))?.click()
            device.findObject(By.text("Search")).click()
            device.findObject(By.text("Add"))
        }
    )
}