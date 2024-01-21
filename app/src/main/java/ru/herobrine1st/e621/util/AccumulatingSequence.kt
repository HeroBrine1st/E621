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

fun <T> Sequence<T>.accumulate(accumulator: suspend SequenceScope<T>.(previous: T, current: T) -> T): Sequence<T> {
    val iterator = this@accumulate.iterator()
    if (!iterator.hasNext()) return emptySequence()
    return sequence {
        var previous = iterator.next()
        for (current in iterator) {
            previous = accumulator(previous, current)
        }
        yield(previous)
    }
}

inline fun <T> Iterable<T>.accumulate(accumulator: AccumulatorScope<T>.(previous: T, current: T) -> T): List<T> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) return emptyList()
    val result = mutableListOf<T>()
    val scope = AccumulatorScope<T> { value -> result.add(value) }
    var previous = iterator.next()
    for (current in iterator) {
        previous = scope.accumulator(previous, current)
    }
    result.add(previous)
    return result
}

fun interface AccumulatorScope<T> {
    fun yield(value: T)
}