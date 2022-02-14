package ru.herobrine1st.e621.api.model

import com.fasterxml.jackson.annotation.JsonValue

data class Sample(
    val has: Boolean, // вщ не ебу что это
    val height: Int,
    val width: Int,
    val url: String,
    val alternates: Map<String, Alternative>
)

data class Alternative(
    val type: AlternativeType,
    val height: Int,
    val width: Int,
    val urls: List<String?> // yes it's really may be nullable
)

enum class AlternativeType(@JsonValue val apiName: String) {
    VIDEO("video"),
    IMAGE("image"),
    //maybe more
}