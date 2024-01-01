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

package ru.herobrine1st.e621.module

import android.content.Context
import android.os.StatFs
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

class MediaModule(val applicationContext: Context) {
    val mediaOkHttpClientLazy = lazy {
        val cacheDir = File(applicationContext.cacheDir, "video").apply { mkdirs() }
        val size = (StatFs(cacheDir.absolutePath).let {
            it.blockCountLong * it.blockSizeLong
        } * DISK_CACHE_PERCENTAGE).toLong()
            .coerceIn(
                MIN_DISK_CACHE_SIZE_BYTES,
                MAX_DISK_CACHE_SIZE_BYTES
            )
        OkHttpClient.Builder()
            .cache(Cache(cacheDir, size))
            .build()
    }

    companion object {
        const val DISK_CACHE_PERCENTAGE = 0.05
        const val MIN_DISK_CACHE_SIZE_BYTES = 10L * 1024 * 1024
        const val MAX_DISK_CACHE_SIZE_BYTES = 1024L * 1024 * 1024
    }
}