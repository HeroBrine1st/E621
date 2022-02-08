package ru.herobrine1st.e621.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties("has_active_children")
data class Relationships(
    val parentId: Int,
    val hasChildren: Boolean,
    val children: List<Int>
)