package ru.herobrine1st.e621.data

import androidx.room.withTransaction
import ru.herobrine1st.e621.database.Database

abstract class BaseRepositoryImpl(val database: Database): BaseRepository {
    override suspend fun <R> withTransaction(block: suspend () -> R) =
        database.withTransaction(block)
}