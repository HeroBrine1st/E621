package ru.herobrine1st.e621.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties("invalid")
data class Tags(
    val general: List<String>,
    val species: List<String>,
    val character: List<String>,
    val copyright: List<String>,
    val artist: List<String>,
    val lore: List<String>,
    val meta: List<String>
) {
    val all by lazy {
        artist + copyright + character + species + general + lore + meta

    }
    val reduced by lazy {
        copyright + artist + character
    }
}