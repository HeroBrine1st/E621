package ru.herobrine1st.e621.api

import androidx.core.text.isDigitsOnly
import ru.herobrine1st.e621.api.model.Post
import java.util.function.Predicate

val integerMetatags: Map<String, (Post) -> Int> = mapOf(
    "id" to { it.id },
    "score" to { it.score.total },
    // Remaining tags are not relevant IMO
)

/**
 * Query - normal search string
 * Micro query - one tag (or metatag) with operator from query
 * Simple micro query - just one tag (or metatag) without operator
 */
fun parseSimpleMicroQuery(microQuery: String): Predicate<Post> {
    if (":" in microQuery) {
        val metatag: String
        var value: String
        microQuery.split(":").let {
            metatag = it[0]
            value = it[1]
        }
        if ((value.isDigitsOnly() && value.isNotEmpty()) // metatag:500
            || (value.substring(1).isDigitsOnly() && value.length > 1) // metatag:>500 metatag:<500
            || (".." in value // metatag:123..456
                    && value.split("..")
                .let { it.size == 2 && it.all { it1 -> it1.isDigitsOnly() } })
        ) {
            val predicate: Predicate<Int>
            if (value.startsWith("<") || value.startsWith(">")) { // metatag:>500 metatag:<500
                val sign = value.substring(0, 1)
                value = value.substring(1)
                if (!value.isDigitsOnly()) {
                    return Predicate { false } // invalid
                }
                val intValue = Integer.parseInt(value)
                predicate = when (sign) {
                    "<" -> {
                        Predicate {
                            it < intValue
                        }
                    }
                    ">" -> {
                        Predicate {
                            it > intValue
                        }
                    }
                    else -> return Predicate { false } // invalid
                }
            } else if (value.isDigitsOnly()) { // metatag:500
                val intValue = Integer.parseInt(value)
                predicate = Predicate {
                    it == intValue
                }
            } else if (".." in value) { // metatag:123..456
                val values = value.split("..")
                assert(values.size == 2)
                val min = Integer.parseInt(values[0])
                val max = Integer.parseInt(values[1])
                val range = min..max
                predicate = Predicate {
                    it in range
                }
            } else {
                return Predicate { false } // invalid
            }
            return parseIntegerMetatag(metatag, predicate)
        } else {
            if (metatag == "rating") {
                val rating = Rating.byAnyName[value]
                return Predicate {
                    it.rating == rating
                }
            }
        }
    } else {
        return Predicate {
            it.tags.all.contains(microQuery)
        }
    }
    return Predicate { false } // invalid or unsupported
}

private fun parseIntegerMetatag(
    metatag: String,
    valuePredicate: Predicate<Int>
): Predicate<Post> {
    return Predicate {
        integerMetatags[metatag]?.invoke(it)
            ?.let { it1 -> valuePredicate.test(it1) } ?: false
    }
}

fun createTagProcessor(query: String): Predicate<Post> {
    val predicates: ArrayList<Predicate<Post>> = ArrayList()
    val microQueries = query.split(" ").toMutableList()
    microQueries.filter { it.startsWith("~") }.let {
        if (it.isNotEmpty()) {
            microQueries.removeAll(it)
            predicates.add(
                it.map { it1 -> parseSimpleMicroQuery(it1.substring(1)) }.reduce {a, b -> a.or(b)}
            )
        }
    }
    microQueries.filter { it.startsWith("-") }.let {
        if (it.isNotEmpty()) {
            microQueries.removeAll(it)
            predicates.addAll(
                it.map { it1 -> parseSimpleMicroQuery(it1.substring(1)).negate() }
            )
        }
    }
    predicates.addAll(microQueries.map { parseSimpleMicroQuery(it) })
    return predicates.reduceOrNull { a, b -> a.and(b) } ?: Predicate { true }
}