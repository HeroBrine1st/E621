package ru.herobrine1st.e621.util

inline fun <T, R> T.letApply(block: T.() -> R): R {
    return this.block()
}