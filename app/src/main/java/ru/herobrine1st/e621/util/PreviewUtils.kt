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

package ru.herobrine1st.e621.util

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import ru.herobrine1st.e621.navigation.config.Config

@RequiresOptIn(message = "This should only be used in previews")
@Retention(AnnotationRetention.BINARY)
annotation class PreviewUtils

@Composable
@PreviewUtils
fun getPreviewComponentContext() = DefaultComponentContext(LifecycleRegistry())

@Composable
@PreviewUtils
fun getPreviewStackNavigator() = object: StackNavigator<Config> {
    override fun navigate(
        transformer: (stack: List<Config>) -> List<Config>,
        onComplete: (newStack: List<Config>, oldStack: List<Config>) -> Unit
    ) {
        onComplete(emptyList(), transformer(emptyList()))
    }
}