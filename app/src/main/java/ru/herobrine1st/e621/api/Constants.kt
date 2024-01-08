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

package ru.herobrine1st.e621.api


const val E621_MAX_POSTS_IN_QUERY = 500

/**
 * Maximum count of items in metatags like "id:1,2,3,4"
 *
 * **See also:** [Source code](https://github.com/e621ng/e621ng/blob/330fb7cb8bc8fcada26c8fb095d96fdf12a4807c/app/logical/parse_value.rb#L55)
 */
const val E621_MAX_ITEMS_IN_RANGE_ENUMERATION = 100