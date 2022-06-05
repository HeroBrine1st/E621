package ru.herobrine1st.e621.api.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
@JsonIgnoreProperties("invalid", "all", "reduced")
data class Tags(
    val general: List<String>,
    val species: List<String>,
    val character: List<String>,
    val copyright: List<String>,
    val artist: List<String>,
    val lore: List<String>,
    val meta: List<String>
) : Parcelable {
    @IgnoredOnParcel
    val all by lazy {
        artist + copyright + character + species + general + lore + meta
    }
    @IgnoredOnParcel
    val reduced by lazy {
        copyright + artist + character
    }
}