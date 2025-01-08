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

package ru.herobrine1st.e621

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.network.NetworkFetcher
import coil3.request.crossfade
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import okio.Path.Companion.toOkioPath
import ru.herobrine1st.e621.module.ApplicationInjectionCompanion
import ru.herobrine1st.e621.util.CoilKtorNetworkFetcher

class Application : Application(), SingletonImageLoader.Factory {
    val injectionCompanion = ApplicationInjectionCompanion(this)

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(this)
        .crossfade(true)
        .components {
            add(
                @OptIn(ExperimentalCoilApi::class) // is not declared, just a warning out of the air
                NetworkFetcher.Factory(
                    networkClient = { CoilKtorNetworkFetcher(HttpClient(CIO)) },
                )
            )
            add(AnimatedImageDecoder.Factory())
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("coilCache").toOkioPath())
                .build()
        }
        .build()
}