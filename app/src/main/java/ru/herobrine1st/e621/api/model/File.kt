package ru.herobrine1st.e621.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import ru.herobrine1st.e621.api.FileType

data class File(
    val width: Int,
    val height: Int,
    @JsonProperty("ext")
    val type: FileType,
    val size: Long,
    val md5: String,
    val url: String
)