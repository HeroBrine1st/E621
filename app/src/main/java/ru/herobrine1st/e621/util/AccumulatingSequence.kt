package ru.herobrine1st.e621.util

fun <T> Sequence<T>.accumulate(accumulator: suspend SequenceScope<T>.(previous: T, current: T) -> T): Sequence<T> {
    val iterator = this@accumulate.iterator()
    if(!iterator.hasNext()) return emptySequence()
    return sequence {
        var previous = iterator.next()
        for(current in iterator) {
            previous = accumulator(previous, current)
        }
        yield(previous)
    }
}