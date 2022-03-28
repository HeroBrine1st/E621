package ru.herobrine1st.e621.api.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import ru.herobrine1st.e621.api.FileType

@Parcelize
@Immutable
@JsonIgnoreProperties("type")
data class Sample(
    val has: Boolean, // вщ не ебу что это
    val height: Int,
    val width: Int,
    val url: String,
    val alternates: Map<String, Alternate>
) : Parcelable {
    @IgnoredOnParcel
    val type by lazy {
        FileType.byExtension[url.splitToSequence(".").lastOrNull()] ?: FileType.UNDEFINED
    }
}
@Parcelize
@Immutable
@JsonIgnoreProperties("normalizedType")
data class Alternate(
    val type: AlternateType,
    val height: Int,
    val width: Int,
    val urls: List<String?> // yes it really may be nullable
) : Parcelable {
    @IgnoredOnParcel
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
