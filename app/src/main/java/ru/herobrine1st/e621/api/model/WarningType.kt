package ru.herobrine1st.e621.api.model

import kotlinx.serialization.SerialName

enum class WarningType {
    @SerialName("warning")
    WARNING,

    @SerialName("record")
    RECORD,

    @SerialName("ban")
    BAN
}