package ru.herobrine1st.e621.util

import android.os.Bundle
import androidx.navigation.NavType
import com.fasterxml.jackson.module.kotlin.readValue
import ru.herobrine1st.e621.api.model.Post

class PostNavType : NavType<Post?>(true) {
    override fun get(bundle: Bundle, key: String): Post? {
        return bundle.getParcelable(key)
    }

    override fun parseValue(value: String): Post {
        return objectMapper.readValue(value)
    }

    override fun put(bundle: Bundle, key: String, value: Post?) {
        bundle.putParcelable(key, value)
    }
}