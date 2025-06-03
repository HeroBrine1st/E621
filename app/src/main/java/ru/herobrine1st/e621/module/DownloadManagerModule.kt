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

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import ru.herobrine1st.e621.api.model.FileType
import ru.herobrine1st.e621.api.model.NormalizedFile

class DownloadManagerModule(context: Context) {
    private val _downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val downloadManager: IDownloadManager = DownloadManagerImpl(_downloadManager)

    private class DownloadManagerImpl(val proxy: DownloadManager) :
        IDownloadManager {
        override fun downloadFile(file: NormalizedFile) {
            if (file.urls.isEmpty()) return
            val url = file.urls.first().toUri()
            val request = DownloadManager.Request(url)
            request.setTitle(url.pathSegments.last())
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val directory = when (file.type) {
                FileType.JPG, FileType.PNG, FileType.GIF -> Environment.DIRECTORY_PICTURES
                FileType.WEBM, FileType.MP4 -> Environment.DIRECTORY_MOVIES
                FileType.SWF, FileType.UNDEFINED -> Environment.DIRECTORY_DOWNLOADS
            }

            request.setDestinationInExternalPublicDir(
                directory,
                "E621/${url.pathSegments.last()}"
            )
            proxy.enqueue(request)
        }
    }
}

interface IDownloadManager {
    fun downloadFile(file: NormalizedFile)
}