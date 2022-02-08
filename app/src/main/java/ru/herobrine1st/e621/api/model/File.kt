package ru.herobrine1st.e621.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class File(
    val width: Int,
    val height: Int,
    @JsonProperty("ext")
    val extension: String?,
    val size: Long,
    val md5: String,
    val url: String
)