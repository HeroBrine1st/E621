package ru.herobrine1st.e621.util

import android.os.Bundle
import ru.herobrine1st.e621.api.Order
import ru.herobrine1st.e621.api.Rating

open class CommonSearchOptions(
    open val tags: List<String>,
    open val order: Order?,
    open val orderAscending: Boolean,
    open val rating: List<Rating>
)

data class SearchOptions(
    override val tags: List<String>,
    override val order: Order,
    override val orderAscending: Boolean,
    override val rating: List<Rating>,
): CommonSearchOptions(tags, order, orderAscending, rating) {
    constructor(bundle: Bundle) : this(
        bundle.getString("tags")!!
            .let { if (it.isBlank()) emptyList() else it.split(",") },
        Order.valueOf(bundle.getString("order")!!),
        bundle.getBoolean("ascending"),
        bundle.getString("rating")!!.split(",").filter { it.isNotBlank() }
            .map { Rating.valueOf(it) })

    fun compileToQuery(): String {
        var query = tags.joinToString(" ")
        (if (orderAscending) this.order.ascendingApiName else this.order.apiName)?.let {
            query += " order:$it"
        }
        if (rating.size < Rating.values().size && rating.isNotEmpty()) {
            query += " " + if (rating.size == 1) {
                "rating:${rating[0].apiName}"
            } else {
                rating.joinToString(" ") { "~rating:${it.apiName}" }
            }
        }
        return query
    }
}