package ru.herobrine1st.e621.util

import android.os.Bundle
import ru.herobrine1st.e621.api.Order
import ru.herobrine1st.e621.api.Rating

data class SearchOptions(
    val tags: List<String>,
    val order: Order,
    val orderAscending: Boolean,
    val rating: List<Rating>
) {
    constructor(bundle: Bundle) : this(
        bundle.getString("tags")!!
            .let { if (it.isBlank()) emptyList() else it.split(",") },
        Order.valueOf(bundle.getString("order")!!),
        bundle.getBoolean("orderAscending"),
        bundle.getString("rating")!!.split(",").map { Rating.valueOf(it) })
}