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

data class NormalizedFile(
    val name: String,
    val width: Int,
    val height: Int,
    val type: FileType,
    val size: Long,
    val urls: List<String>
) {
    constructor(name: String, width: Int, height: Int, type: FileType, size: Long, url: String) :
            this(name, width, height, type, size, listOf(url))

    constructor(file: File) :
            this("original", file.width, file.height, file.type, file.size, file.url)

    constructor(file: Sample) :
            this("sample", file.width, file.height, file.type, 0, file.url)

    constructor(name: String, file: Alternative) :
            this(name, file.width, file.height, file.type, 0, file.urls.filterNotNull())
}