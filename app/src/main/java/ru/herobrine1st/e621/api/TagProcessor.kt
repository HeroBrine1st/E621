package ru.herobrine1st.e621.api

import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.Rating
import java.util.function.Predicate
import java.util.regex.Pattern


// TODO add another for these:
// date:month..year
// filesize:200KB..300KB
val integerMetaTagPattern: Pattern =
    Pattern.compile("^([^\\s:]+):([<>]=?)?(\\d+)(?:\\.\\.)?(\\d+)?\$")

val operation = { i1: Int, operator: String, i2: Int ->
    when (operator) {
        ">=" -> i1 >= i2
        ">" -> i1 > i2
        "<=" -> i1 <= i2
        "<" -> i1 < i2
        "==" -> i1 == i2
        else -> false
    }
}

@Suppress("SpellCheckingInspection")
val metaTagToNumber = mapOf<String, (Post) -> Int>(
    "id" to { it.id },
    "score" to { it.score.total },
    "favcount" to { it.favoriteCount },
    "comment_count" to { it.commentCount }
    // others ?
)

fun createPredicateFromTag(tag: String): Predicate<Post> {
    val matcher = integerMetaTagPattern.matcher(tag)
    if (matcher.matches() &&
        // Regexes have condition statements, but java doesn't support it
        // Either 2nd or 4th group, not both
        (matcher.group(2) == null || matcher.group(4) == null)
    ) {
        val mappingFrom = metaTagToNumber[matcher.group(1)!!]
            ?: return Predicate { false } // If not found then not matching anything
        val operator = matcher.group(2) ?: "=="
        if (matcher.group(4) == null) {
            val value = matcher.group(3)!!.toInt()
            return Predicate {
                return@Predicate operation(mappingFrom(it), operator, value)
            }
        } else {
            val range = matcher.group(3)!!.toInt().let {
                it..(matcher.group(4)!!.toInt())
            }
            return Predicate {
                mappingFrom(it) in range
            }
        }
    } else if (tag.startsWith("rating:") || tag.startsWith("r:")) {
        val expectedRating = Rating.byAnyName[tag.split(":")[1]]
        return Predicate {
            it.rating == expectedRating
        }
    } else {
        return Predicate {
            it.tags.all.contains(tag)
        }
    }
}

fun createTagProcessor(query: String): Predicate<Post> {
    val tags = query.split(" ")
    // We should remove "-" and "~" either here or when passing to createPredicateFromTag
    // If first, creation of allOf involves adding these prefixes
    val anyOf = tags.filter { it.startsWith("~") }
    val nothingOf = tags.filter { it.startsWith("-") }
    val allOf = tags - anyOf.toSet() - nothingOf.toSet()

    val first = anyOf.map {
        createPredicateFromTag(it.removePrefix("~"))
    }.reduceOrNull { a, b -> a.or(b) } ?: Predicate { true }
    val second = nothingOf.map {
        createPredicateFromTag(it.removePrefix("-"))
    }.reduceOrNull { a, b -> a.and(b) }?.negate() ?: Predicate { true }
    val third = allOf.map { createPredicateFromTag(it) }
        .reduceOrNull { a, b -> a.and(b) } ?: Predicate { true }

    return first.and(second).and(third)
}