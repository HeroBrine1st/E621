/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.enumeration

enum class AuthState {
    /**
     * No credentials are stored
     */
    NO_DATA,

    /**
     * Initial state
     */
    LOADING,

    /**
     * Credentials are stored and valid
     */
    AUTHORIZED,

    /**
     * Couldn't perform a request to the API
     */
    IO_ERROR,

    /**
     * Internal error occurred
     */
    DATABASE_ERROR,

    /**
     * API error occurred while trying to authenticate
     */
    UNAUTHORIZED
}