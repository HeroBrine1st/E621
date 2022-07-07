package ru.herobrine1st.e621.api.model

import java.time.OffsetDateTime

data class WikiPage(
    val id: Int,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime?,
    val title: String,
    val body: String,
    val creatorId: Int,
    val creatorName: String? = null,
    val updaterId: Int,
    val isLocked: Boolean,
    val isDeleted: Boolean,
    val otherNames: List<String>,
    val categoryName: Int
)
