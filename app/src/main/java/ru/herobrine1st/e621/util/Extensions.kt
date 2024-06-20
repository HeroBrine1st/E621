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

package ru.herobrine1st.e621.util

import android.app.Activity
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.model.Tag
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// Like also, but debug
// TODO setting to enable debug in preferences
@OptIn(ExperimentalContracts::class)
inline fun <T> T.debug(block: T.() -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (BuildConfig.DEBUG) this.block()
    return this
}

@OptIn(ExperimentalContracts::class)
inline fun debug(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (BuildConfig.DEBUG) block()
}

fun Activity.restart() {
    val intent = intent
    finish()
    startActivity(intent)
}

inline fun <T> T.runIf(condition: Boolean, block: T.() -> T): T {
    return if (condition) block(this) else this
}

val Tag.text
    get() = if (BuildConfig.HIDE_UNDERSCORES_FROM_USER) value.replace('_', ' ')
    else value

