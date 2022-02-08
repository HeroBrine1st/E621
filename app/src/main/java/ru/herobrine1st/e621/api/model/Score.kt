package ru.herobrine1st.e621.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import ru.herobrine1st.e621.api.model.Score
import ru.herobrine1st.e621.api.model.Tags
import ru.herobrine1st.e621.api.model.Relationships
import ru.herobrine1st.e621.api.model.Post

data class Score(
    val up: Int,
    val down: Int,
    val total: Int
)