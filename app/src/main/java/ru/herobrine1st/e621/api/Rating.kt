package ru.herobrine1st.e621.api

import androidx.annotation.StringRes
import com.fasterxml.jackson.annotation.JsonValue
import ru.herobrine1st.e621.R

enum class Rating(@StringRes val descriptionId: Int, val apiName: String) {
    SAFE(R.string.rating_safe, "safe"),
    QUESTIONABLE(R.string.rating_questionable, "questionable"),
    EXPLICIT(R.string.rating_explicit, "explicit");

    @JsonValue
    val shortName = apiName.substring(0, 1)

    companion object {
        val byAnyName: Map<String, Rating> = HashMap<String, Rating>().apply {
            values().forEach {
                put(it.apiName.lowercase(), it) // lowercase just in case
                put(it.shortName.lowercase(), it)
            }
        }
    }
}
