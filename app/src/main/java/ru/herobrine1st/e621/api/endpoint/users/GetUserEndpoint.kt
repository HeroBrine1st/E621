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

package ru.herobrine1st.e621.api.endpoint.users

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import ru.herobrine1st.e621.api.HttpMethod
import ru.herobrine1st.e621.api.HttpMethodType.GET
import ru.herobrine1st.e621.api.JsonFormatSuffix
import ru.herobrine1st.e621.api.endpoint.APIEndpoint

// TODO proper model
@Serializable
@HttpMethod(GET)
@JsonFormatSuffix
@Resource("/users/{name}")
data class GetUserEndpoint(
    @SerialName("name") val name: String
) : APIEndpoint<Unit, JsonObject>
