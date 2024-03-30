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

package ru.herobrine1st.e621.navigation.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.module.IDownloadManager

class PostMediaComponent(
    private val post: Post,
    initialFile: NormalizedFile,
    private val downloadManager: IDownloadManager,
    componentContext: ComponentContext,
) : ComponentContext by componentContext {
    val files = post.files

    init {
        check(initialFile in files) { "Initial file does not belong to post" }
    }

    var currentFile by mutableStateOf(initialFile)
        private set

    fun setFile(file: NormalizedFile) {
        check(file in files) { "File does not belong to post" }
        currentFile = file
    }

    fun downloadFile() {
        downloadManager.downloadFile(post.normalizedFile)
    }
}