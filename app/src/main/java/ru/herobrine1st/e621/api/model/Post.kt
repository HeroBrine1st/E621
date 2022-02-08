package ru.herobrine1st.e621.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import ru.herobrine1st.e621.api.Rating
import ru.herobrine1st.e621.api.model.Score
import ru.herobrine1st.e621.api.model.Tags
import ru.herobrine1st.e621.api.model.Relationships
import ru.herobrine1st.e621.api.model.Post

@JsonIgnoreProperties(ignoreUnknown = true)
data class Post(
    val id: Int,
    val file: File,
    val score: Score,
    val tags: Tags,
    val rating: Rating,
    val description: String,
    val relationships: Relationships
)