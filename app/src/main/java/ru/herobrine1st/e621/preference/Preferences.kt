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

@file:OptIn(ExperimentalSerializationApi::class)

package ru.herobrine1st.e621.preference

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoOneOf

@Serializable
data class Preferences(
    @ProtoNumber(1) val blacklistEnabled: Boolean = true,
    @ProtoNumber(2) val dataSaverModeEnabled: Boolean = false,
    @ProtoNumber(3) val dataSaverDisclaimerShown: Boolean = false,
    @ProtoNumber(4) val showRemainingTimeMedia: Boolean = true,
    @ProtoNumber(5) val muteSoundOnMedia: Boolean = true,
    @ProtoNumber(6) val auth: AuthorizationCredentials? = null,
    @ProtoNumber(7) val safeModeEnabled: Boolean = true,
    @ProtoNumber(8) val safeModeDisclaimerShown: Boolean = false,
    @ProtoNumber(9) val licenseAndNonAffiliationDisclaimerShown: Boolean = false,
    @ProtoNumber(10) val proxy: Proxy? = null,
    @ProtoNumber(11) val autoplayOnPostOpen: Boolean = true,
    @ProtoNumber(12) val autocompleteEnabled: Boolean = true,
    @ProtoOneOf val postsColumns: PostsColumns = PostsColumns.Fixed(1)
) {
    @Serializable
    sealed interface PostsColumns {
        @JvmInline
        @Serializable
        value class Adaptive(@ProtoNumber(13) val widthDp: Float) : PostsColumns

        @JvmInline
        @Serializable
        value class Fixed(@ProtoNumber(14) val columnCount: Int) : PostsColumns
    }
}

@Serializable
data class AuthorizationCredentials(
    val username: String,
    val apiKey: String,
    // TODO remove default value and migration after a couple of releases
    val id: Int = -1
)

@Serializable
data class Proxy(
    val type: ProxyType,
    val hostname: String,
    val port: Int,
    val enabled: Boolean = true,
    val auth: ProxyAuth?
)

@Serializable
data class ProxyAuth(
    val username: String,
    val password: String
)

enum class ProxyType {
    SOCKS5;
}