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



package ru.herobrine1st.e621.api.model

import androidx.annotation.StringRes
import ru.herobrine1st.e621.R

enum class Order(
    @StringRes val descriptionId: Int, val apiName: String?, val supportsAscending: Boolean = true,
    val supportsPaging: Boolean = true,
    val ascendingApiName: String = apiName + "_asc"
) {
    NEWEST_TO_OLDEST(R.string.order_none, null, ascendingApiName = "id"),
    //    OLDEST_TO_NEWEST(R.string.order_id, "id", supportsAscending = false),
    SCORE(R.string.order_score, "score"),
    FAVORITE_COUNT(R.string.order_favcount, "favcount"),
    TAG_COUNT(R.string.order_tagcount, "tagcount"),
    COMMENT_COUNT(R.string.order_comment_count, "comment_count"),
    RESOLUTION(R.string.order_mpixels, "mpixels"),
    FILE_SIZE(R.string.order_filesize, "filesize"),
    WIDEST_FIRST(R.string.order_landscape, "landscape", ascendingApiName = "portrait"),
    //    WIDEST_LAST(R.string.order_portrait, "portrait", supportsAscending = false),
    DURATION(R.string.order_duration, "duration"),
    CHANGE(R.string.order_change, "change", supportsAscending = false),
    RANDOM(R.string.order_random, "random", supportsAscending = false, supportsPaging = false)
}