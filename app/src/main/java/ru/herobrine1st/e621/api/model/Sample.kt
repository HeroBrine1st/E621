package ru.herobrine1st.e621.api.model

import com.fasterxml.jackson.annotation.JsonValue
import ru.herobrine1st.e621.api.FileType

data class Sample(
    val has: Boolean, // вщ не ебу что это
    val height: Int,
    val width: Int,
    val url: String,
    val alternates: Map<String, Alternate>
) {
    val type by lazy {
        FileType.byExtension[url.splitToSequence(".").lastOrNull()] ?: FileType.UNDEFINED
    }
}

data class Alternate(
    val type: AlternateType,
    val height: Int,
    val width: Int,
    val urls: List<String?> // yes it really may be nullable
) {
    val normalizedType by lazy {
        urls.mapNotNull {
            FileType.byExtension[it?.splitToSequence(".")?.lastOrNull()]
        }.firstOrNull() ?: FileType.UNDEFINED
    }
}

enum class AlternateType(@JsonValue val apiName: String) {
    VIDEO("video"),
    IMAGE("image"),
    //maybe more
}