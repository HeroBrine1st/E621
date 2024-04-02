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

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.DTextTag.Companion.stylizeNullable
import ru.herobrine1st.e621.util.accumulate
import ru.herobrine1st.e621.util.debug

private const val TAG = "BBCodeProcessor"

val BOLD = SpanStyle(
    fontWeight = FontWeight.Bold
)

val ITALIC = SpanStyle(
    fontStyle = FontStyle.Italic
)

const val WIKI_PAGE_STRING_ANNOTATION_TAG = "WIKI_PAGE"

@Immutable
sealed interface MessageData {
    fun isEmpty(): Boolean
}

/**
 * Regular text in message
 * @param text Part of text in message.
 */
@Immutable
data class MessageText(
    val text: AnnotatedString,
) : MessageData {

    constructor(text: String) : this(AnnotatedString(text))

    override fun isEmpty() = text.isEmpty()

    fun trim() = MessageText(text.trim() as AnnotatedString)
}

/**
 * Quote in message
 * @param author name and id of author of the quote
 * @param data the quote
 */
@Immutable
data class MessageQuote(
    val author: Author?,
    val data: List<MessageData>,
) : MessageData {
    override fun isEmpty(): Boolean = data.isEmpty()

    @Immutable
    data class Author(
        val userName: String,
        val userId: Int,
    )
}

private sealed interface DTextTag {
    val name: String

    /**
     * Stylize provided data by this tag. This function should usually be called last in every
     * recursion frame (i.e. before "return").
     *
     * @param data list of parsed [MessageData]
     * @return styled list of [MessageData]
     */
    fun stylize(data: List<MessageData>): List<MessageData>

    sealed class Styled(override val name: String, private val style: SpanStyle) : DTextTag {
        override fun stylize(data: List<MessageData>): List<MessageData> {
            return data.map {
                (it as? MessageText)?.text?.let { text ->
                    MessageText(AnnotatedString.Builder().apply {
                        withStyle(style) {
                            append(text)
                        }
                    }.toAnnotatedString())
                } ?: it
            }
        }
    }

    data object Bold : Styled("b", BOLD)
    data object Italic : Styled("i", ITALIC)

    data object Quote : DTextTag {
        override val name: String = "quote"

        override fun stylize(data: List<MessageData>): List<MessageData> {
            val collapsed = data.collapseMessageTexts().toMutableList()
            val first = collapsed[0]
            val author: MessageQuote.Author?
            if (first is MessageText) {
                val header = parseQuoteHeader(first.text.text)
                if (header != null) {
                    author = header.first
                    val startIndex = header.second
                    collapsed[0] = MessageText(
                        first.text.subSequence(startIndex, first.text.length)
                            .trim() as AnnotatedString
                    )
                } else author = null
            } else author = null
            return listOf(MessageQuote(author, collapsed))
        }
    }

    class Unknown(
        override val name: String,
        val commaAttribute: String,
        val property: String
    ) : DTextTag {
        override fun stylize(data: List<MessageData>): List<MessageData> {
            val startingTag =
                "[$name" +
                        (if (commaAttribute.isNotBlank()) ",$commaAttribute" else "") +
                        (if (property.isNotBlank()) "=$property" else "") +
                        "]"
            return buildList(capacity = data.size + 2) {
                add(MessageText(startingTag))
                addAll(data)
                add(MessageText("[/$name]"))
            }
        }
    }

    companion object {
        /**
         * Get instance by name and attributes.
         *
         * Typical tag looks like this:
         *
         * `[name,commaAttribute=property]`
         *
         * Where only name is mandatory.
         */
        fun fromName(
            name: String,
            commaAttribute: String,
            property: String
        ): DTextTag = when (name) {
            "b" -> Bold
            "i" -> Italic
            "quote" -> Quote
            else -> Unknown(name, commaAttribute, property)
        }

        fun DTextTag?.stylizeNullable(data: List<MessageData>) = this?.stylize(data) ?: data
    }
}

/**
 * Handle:
 * 1. ``[tag]``, including ``[tag,attr]``, ``[tag=attr]`` and ``[tag,attr=attr]``
 * 2. ``[/tag]``
 * 3. ``[[link]]`` and ``[[link|text]]``
 */
@Suppress("KDocUnresolvedReference")
val pattern =
    Regex("""\[(?:([^=\[\]/,]+)(?:,([^=\[\]/]+))?(?:=([^\[\]/]+))?|/([^\[\]/]+)|\[([^\[\]/|]+)(?:\|([^\[\]/]+))?])]""")

// "name":/user/show/0 said:
// "name":/users/0 said:
// name said:
val quotePattern = Regex("""(?:"?([^"\n]+)"?:/user(?:s|/show)/(\d+)|(\S+)) said:\r?\n""")

fun parseBBCode(input: String): List<MessageData> {
    val (parsed, end) = parseBBCodeInternal(input, null, 0)
    assert(end == input.length) {
        "Parser hasn't reached end of string: expected ${input.length}, actual $end"
    }
    return parsed.collapseMessageTexts()
}

@OptIn(ExperimentalTextApi::class)
private fun parseBBCodeInternal(
    input: String,
    currentTag: DTextTag?,
    initialStart: Int,
): Pair<List<MessageData>, Int> {
    val output = mutableListOf<MessageData>()
    var start = initialStart

    var tagClosed = false

    while (start < input.length) {
        val match: MatchResult = pattern.find(input, startIndex = start) ?: break
        if (match.range.first > start) { // Include text ([tag] -> recurse -> include_this_text[\tag])
            output += MessageText(input.substring(start, match.range.first))
        }
        start = match.range.last + 1

        val openingTag = match.groupValues[1]
        if (openingTag != "") {
            val attribute = match.groupValues[2]
            val property = match.groupValues[3]
            output += parseBBCodeInternal(
                input,
                DTextTag.fromName(openingTag, attribute, property),
                match.range.last + 1
            ).also {
                start = it.second
            }.first
            continue
        }

        val closingTag = match.groupValues[4]
        if (closingTag != "") {
            if (closingTag == currentTag?.name) {
                tagClosed = true
                break
            } else {
                debug {
                    Log.d(TAG, "Invalid closing tag found at indexes ${match.range}")
                    Log.d(TAG, input)
                }
                output += MessageText(AnnotatedString(match.groupValues[0]))
                continue
            }
        }

        val link = match.groupValues[5]
        if (link != "") {
            val hyper = match.groupValues[6]
            output += MessageText(
                AnnotatedString.Builder().apply {
                    withAnnotation(UrlAnnotation("${BuildConfig.DEEP_LINK_BASE_URL}/wiki_pages/show_or_new?title=$link")) {
                        withAnnotation(WIKI_PAGE_STRING_ANNOTATION_TAG, link) {
                            append(hyper.ifBlank { link })
                        }
                    }
                }.toAnnotatedString()
            )
            continue
        }
        Log.w(TAG, "Catchall triggered on `${match.groupValues[0]}`")
        output += MessageText(AnnotatedString(match.groupValues[0]))
    }

    if (!tagClosed) {
        output.add(MessageText(AnnotatedString(input.substring(start))))
        start = input.length
    }
    return currentTag.stylizeNullable(output) to start
}

private fun parseQuoteHeader(input: String): Pair<MessageQuote.Author, Int>? {
    val match = quotePattern.matchAt(input, index = 0) ?: return null
    return MessageQuote.Author(
        match.groupValues[1].ifEmpty { match.groupValues[3] },
        match.groupValues[2].ifEmpty { "-1" }.toInt()
    ) to match.range.last + 1
}

/**
 * Fix message data in a way that it can be properly displayed in Column, i.e. concatenate row of [MessageText]s
 * into one object, so that there would be no line break at tag boundary.
 *
 * Order of text and other [MessageData] is kept.
 *
 * @return Optimized list of [MessageData]
 */
private fun List<MessageData>.collapseMessageTexts(): List<MessageData> {
    // TD;DR accumulate MessageText until another MessageData is found, then continue with another accumulator)
    return this
        .filter { (it as? MessageText)?.text?.isNotEmpty() != false }
        .accumulate { previous, current ->
            if (previous is MessageText && current is MessageText) {
                MessageText(previous.text + current.text)
            } else {
                yield(previous)
                current
            }
        }
        .map { (it as? MessageText)?.trim() ?: it }
}