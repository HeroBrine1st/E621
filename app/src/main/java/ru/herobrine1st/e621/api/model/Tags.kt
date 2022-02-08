package ru.herobrine1st.e621.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties("invalid")
data class Tags(
    var general: List<String>,
    var species: List<String>,
    var character: List<String>,
    var copyright: List<String>,
    var artist: List<String>,
    var lore: List<String>,
    var meta: List<String>
) {
    val all by lazy {
        artist + copyright + character + species + general + lore + meta

    }
    val reduced by lazy {
        copyright + artist + character
    }
}