package ru.herobrine1st.e621.api.model

data class Relationships(
    val parentId: Int,
    val hasChildren: Boolean,
    val hasActiveChildren: Boolean,
    val children: List<Int>
)