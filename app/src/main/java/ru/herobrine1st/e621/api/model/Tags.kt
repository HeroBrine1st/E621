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
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import ru.herobrine1st.e621.api.Tokens

@Parcelize
@Immutable
@JsonIgnoreProperties("invalid", "all", "reduced")
data class Tags(
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

// FIXME replace data class with value class
//       blocked by jackson-module-kotlin
//       https://github.com/FasterXML/jackson-module-kotlin/issues/650
//       https://github.com/FasterXML/jackson-module-kotlin/issues/199
//       Data class may have serious performance or (likely) memory impact
// Also deserializers may become useless because of support in Jackson
@Parcelize
@JsonDeserialize(using = TagDeserializer::class)
data class Tag(val value: String) : Parcelable {
    // "Alternative" is proposed by ChatGPT and should be read like "alternative tags".
    // It is not ideal, but I couldn't figure any better
    inline val asAlternative get() = Tokens.ALTERNATIVE + value
    inline val asExcluded get() = Tokens.EXCLUDED + value
}

class TagDeserializer : JsonDeserializer<Tag>() {
    override fun deserialize(p: JsonParser, ctx: DeserializationContext): Tag {
        return Tag(ctx.readValue(p, String::class.java))
    }
}

class TagListDeserializer : JsonDeserializer<List<Tag>>() {
    override fun deserialize(p: JsonParser, ctx: DeserializationContext): List<Tag> {
        val tree = ctx.readTree(p)
        if (!tree.isArray) throw MismatchedInputException.from(
            p,
            List::class.java,
            "Node is not an array of tags"
        )
        return tree.map {
            if (!it.isTextual) throw MismatchedInputException.from(
                p,
                Tag::class.java,
                "Node is not a tag"
            )
            Tag(it.textValue())
        }
    }
}
