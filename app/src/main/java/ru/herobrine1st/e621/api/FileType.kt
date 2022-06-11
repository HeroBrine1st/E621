package ru.herobrine1st.e621.api

import com.fasterxml.jackson.annotation.JsonValue
import okhttp3.internal.toImmutableMap

@Suppress("unused")
enum class FileType(
    @JsonValue val extension: String,
    val isSupported: Boolean = true,
    val isImage: Boolean = false,
    val isVideo: Boolean = false,
    val weight: Byte = 0 // to sort by sample type and then by resolution
) {
    JPG("jpg", isImage = true),
    PNG("png", isImage = true),
    GIF("gif", isImage = true, weight = 1),
    SWF("swf", isSupported = false),
    WEBM("webm", isVideo = true, weight = 2),
    UNDEFINED("", isSupported = false);

    val isNotImage = !isImage
    val isNotVideo = !isVideo

    companion object {
        val byExtension = mutableMapOf<String, FileType>().apply {
            values()
                .forEach { this[it.extension] = it }
        }.toImmutableMap()
    }
}

