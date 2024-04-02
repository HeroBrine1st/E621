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

package ru.herobrine1st.e621.navigation

import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

inline fun <T : Any> StackNavigator<T>.pushIndexed(crossinline create: (index: Int) -> T) =
    navigate {
        it + create(it.size)
    }

@Suppress("FunctionName")
fun LifecycleOwner.LifecycleScope(context: CoroutineContext = Dispatchers.Main.immediate + Job()): CoroutineScope {
    val coroutineScope = CoroutineScope(context)
    lifecycle.doOnDestroy {
        coroutineScope.cancel()
    }
    return coroutineScope
}