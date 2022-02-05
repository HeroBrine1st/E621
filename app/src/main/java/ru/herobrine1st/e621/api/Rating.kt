package ru.herobrine1st.e621.api

import androidx.annotation.StringRes
import ru.herobrine1st.e621.R

enum class Rating(@StringRes val descriptionId: Int, val apiName: String?) {
    SAFE(R.string.rating_safe, "safe"),
    QUESTIONABLE(R.string.rating_questionable, "questionable"),
    EXPLICIT(R.string.rating_explicit, "explicit")
}
