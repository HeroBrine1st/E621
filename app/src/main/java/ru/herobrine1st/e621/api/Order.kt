package ru.herobrine1st.e621.api

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
    FILESIZE(R.string.order_filesize, "filesize"),
    WIDEST_FIRST(R.string.order_landscape, "landscape", ascendingApiName = "portrait"),
    //    WIDEST_LAST(R.string.order_portrait, "portrait", supportsAscending = false),
    DURATION(R.string.order_duration, "duration"),
    CHANGE(R.string.order_change, "change", supportsAscending = false),
    RANDOM(R.string.order_random, "random", supportsAscending = false, supportsPaging = false)
}