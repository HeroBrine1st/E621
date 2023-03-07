/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.herobrine1st.e621.util

import android.os.Bundle
import androidx.navigation.NavType
import com.fasterxml.jackson.module.kotlin.readValue
import ru.herobrine1st.e621.api.model.Post


class PostNavType : NavType<Post?>(true) {
    override fun get(bundle: Bundle, key: String): Post? {
        return bundle.getParcelableCompat(key)
    }

    override fun parseValue(value: String): Post {
        return objectMapper.readValue(value)
    }

    override fun put(bundle: Bundle, key: String, value: Post?) {
        bundle.putParcelable(key, value)
    }
}