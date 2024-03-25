/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.api.endpoint.posts

import io.ktor.resources.Resource
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer
import ru.herobrine1st.e621.api.HttpMethod
import ru.herobrine1st.e621.api.HttpMethodType
import ru.herobrine1st.e621.api.endpoint.APIEndpoint
import ru.herobrine1st.e621.api.model.CommentBB
import ru.herobrine1st.e621.api.model.PostId


@Serializable
@HttpMethod(HttpMethodType.GET)
@Resource("/comments.json")
data class GetPostCommentsDTextEndpoint(
    @SerialName("search[post_id]") val id: PostId,
    @SerialName("page") val page: Int,
    @SerialName("limit") val limit: Int, // Default unknown. Maybe 75, but I doubt
    @SerialName("group_by") val groupBy: String = "comment",
) : APIEndpoint<Unit, GetPostCommentsDTextEndpoint.Response> {

    @Serializable(with = ResponseSerializer::class)
    data class Response(val comments: List<CommentBB>)

    class ResponseSerializer : KSerializer<Response> {
        override val descriptor: SerialDescriptor
            get() = buildClassSerialDescriptor("GetPostCommentsDTextEndpoint.ResponseSerializer")

        override fun deserialize(decoder: Decoder): Response {
            decoder as JsonDecoder

            return Response(
                decoder.json.decodeFromJsonElement<List<CommentBB>>(
                    when (val element = decoder.decodeJsonElement()) {
                        is JsonArray -> element
                        is JsonObject -> element["comments"]
                            ?: throw IllegalArgumentException("Comments field is not found in wrapped comments response")

                        is JsonPrimitive -> throw IllegalArgumentException("Expected JsonArray/JsonObject, got primitive in comments response as root element")
                    }
                )
            )
        }

        override fun serialize(encoder: Encoder, value: Response) {
            encoder as JsonEncoder
            encoder.encodeSerializableValue(serializer<List<CommentBB>>(), value.comments)
        }

    }

    init {
        require(groupBy == "comment")
    }
}