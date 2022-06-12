package ru.herobrine1st.e621.data

interface BaseRepository {
    suspend fun <R> withTransaction(block: suspend () -> R): R
}