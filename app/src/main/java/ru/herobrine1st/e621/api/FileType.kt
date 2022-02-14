package ru.herobrine1st.e621.api

import com.fasterxml.jackson.annotation.JsonValue

enum class FileType(@JsonValue val extension: String, val isSupported: Boolean = true, val isImage: Boolean = false, val isVideo: Boolean = false) {
    JPG("jpg", isImage = true),
    PNG("png", isImage = true),
    GIF("gif", isImage = true),
    SWF("swf", isSupported = false),
    WEBM("webm",  isVideo = true);
}