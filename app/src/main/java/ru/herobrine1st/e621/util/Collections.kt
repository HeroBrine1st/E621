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

/**
 * Equivalent of flatMap(mapper).get(index), but without list allocations
 */
inline fun <T, K> List<T>.getAtIndex2DOrNull(index: Int, mapper: (T) -> List<K>): K? {
    var accumulatedSize = 0
    forEach {
        val list = mapper(it)
        if (accumulatedSize + list.size < index) {
            accumulatedSize += list.size
            return@forEach
        }
        return list[index - accumulatedSize]
    }
    return null
}

/**
 * Equivalent of flatMap(mapper).indexOfFirst(predicate), but without list allocations
 */
inline fun <T, K> List<T>.indexOfFirst2D(mapper: (T) -> List<K>, predicate: (K) -> Boolean): Int {
    var accumulatedSize = 0
    forEach {
        val list = mapper(it)
        val index = list.indexOfFirst(predicate)
        if (index != -1) return accumulatedSize + index
        accumulatedSize += list.size
    }
    return -1
}