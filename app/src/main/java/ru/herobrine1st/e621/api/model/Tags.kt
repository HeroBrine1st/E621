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

package ru.herobrine1st.e621.api.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
@JsonIgnoreProperties("invalid", "all", "reduced")
data class Tags(
    // STOPSHIP not tested
    @JsonDeserialize(using = TagListDeserializer::class) val general: List<Tag>,
    @JsonDeserialize(using = TagListDeserializer::class) val species: List<Tag>,
    @JsonDeserialize(using = TagListDeserializer::class) val character: List<Tag>,
    @JsonDeserialize(using = TagListDeserializer::class) val copyright: List<Tag>,
    @JsonDeserialize(using = TagListDeserializer::class) val artist: List<Tag>,
    @JsonDeserialize(using = TagListDeserializer::class) val lore: List<Tag>,
    @JsonDeserialize(using = TagListDeserializer::class) val meta: List<Tag>
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

@JvmInline
@Parcelize
@JsonDeserialize(using = TagDeserializer::class)
value class Tag(val value: String) : Parcelable

class TagDeserializer : JsonDeserializer<Tag>() {
    override fun deserialize(p: JsonParser, ctx: DeserializationContext): Tag {
        return Tag(ctx.readValue(p, String::class.java))
    }
}

class TagListDeserializer : JsonDeserializer<List<Tag>>() {
    override fun deserialize(p: JsonParser, ctx: DeserializationContext): List<Tag> {
        val tree = ctx.readTree(p)
        assert(!tree.isArray) { "Node is not an array of tags" }
        return tree.map {
            assert(!it.isTextual) { "Node is not a tag" }
            Tag(it.textValue())
        }
    }
}
