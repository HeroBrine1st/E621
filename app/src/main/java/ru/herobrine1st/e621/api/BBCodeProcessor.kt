/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.util.accumulate
import ru.herobrine1st.e621.util.debug

private const val TAG = "BBCodeProcessor"

val BOLD = SpanStyle(
    fontWeight = FontWeight.Bold
)

val ITALIC = SpanStyle(
    fontStyle = FontStyle.Italic
)

// TODO replace it with something more suitable
// We're using functional programming, after all (I'm kidding xD, I cannot remove data classes)
sealed class BBCodeTag(val name: String) {
    abstract fun stylize(s: AnnotatedString): AnnotatedString

    object Bold : BBCodeTag("b") {
        override fun stylize(s: AnnotatedString) = AnnotatedString.Builder().apply {
            withStyle(BOLD) {
                append(s)
            }
        }.toAnnotatedString()
    }

    object Italic : BBCodeTag("i") {
        override fun stylize(s: AnnotatedString) = AnnotatedString.Builder().apply {
            withStyle(ITALIC) {
                append(s)
            }
        }.toAnnotatedString()
    }
}

@Immutable
sealed interface MessageData<T : MessageData<T>> {
    fun isEmpty(): Boolean

    // For cases where full message does not fit (for example, when there's no MessageText at all but preview required)
    // Rare case, but should implement fallback
    @StringRes
    fun getDescription(): Int

    fun stylize(tag: BBCodeTag): T
}

/**
 * Regular text in message
 * @param text Part of text in message.
 */
@Immutable
data class MessageText(
    val text: AnnotatedString,
) : MessageData<MessageText> {
    override fun isEmpty() = text.isEmpty()
    override fun getDescription(): Int = R.string.message_text
    override fun stylize(tag: BBCodeTag) = MessageText(tag.stylize(text))

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
    val data: List<MessageData<*>>
) : MessageData<MessageQuote> {
    override fun isEmpty(): Boolean = data.isEmpty()
    override fun getDescription(): Int = R.string.message_quote
    override fun stylize(tag: BBCodeTag): MessageQuote = this

    @Immutable
    data class Author(
        val userName: String,
        val userId: Int
    )
}

/**
 * Handle:
 * 1. ``[tag]``
 * 2. ``[/tag]``
 * 3. ``[[link]]``
 * 4. ``[[link|text]]``
 */
@Suppress("KDocUnresolvedReference")
val pattern = Regex("""\[(?:([^\[\]/]+)|/([^\[\]/]+)|\[([^\[\]/]+)(?:\|([^\[\]/]+))?)]""")

// "name":/user/show/0 said:
// "name":/users/0 said:
// name said:
val quotePattern = Regex("""(?:"?([^"\n]+)"?:/user(?:s|/show)/(\d+)|(\S+)) said:\r?\n""")

fun parseBBCode(input: String): List<MessageData<*>> {
    val (parsed, end) = parseBBCodeInternal(input, null, 0)
    assert(end == input.length) {
        "Parser hasn't reached end of string: expected ${input.length}, actual $end"
    }
    return parsed.collapseMessageTexts()
}

private fun parseBBCodeInternal(
    input: String,
    currentTag: String?,
    initialStart: Int
): Pair<List<MessageData<*>>, Int> {
    assert(currentTag != "")
    val output = mutableListOf<MessageData<*>>()
    var start = initialStart

    val quoteAuthor = when (currentTag) {
        "quote" -> parseQuoteHeader(input, start)
            ?.also { start = it.second }
            ?.first
        else -> null
    }

    var tagClosed = false

    while (start < input.length) {
        val match: MatchResult = pattern.find(input, startIndex = start) ?: break
        if (match.range.first > start) { // Include text ([tag] -> recurse -> include_this_text[\tag])
            output += MessageText(AnnotatedString(input.substring(start, match.range.first)))
        }
        start = match.range.last + 1

        val openingTag = match.groupValues[1]
        if (openingTag != "") {
            output += parseBBCodeInternal(input, openingTag, match.range.last + 1).also {
                start = it.second
            }.first
            continue
        }

        val closingTag = match.groupValues[2]
        if (closingTag != "") {
            if (closingTag == currentTag) {
                tagClosed = true
                break
            } else {
                debug {
                    Log.d(TAG, "Invalid closing tag found at indexes ${match.range}")
                    Log.d(TAG, input)
                }
                output += MessageText(AnnotatedString("[/$closingTag]"))
            }
            continue
        }

        // Catchall, as not all groups are used
        output += MessageText(AnnotatedString(match.groupValues[0]))
    }

    if (!tagClosed) {
        output.add(MessageText(AnnotatedString(input.substring(start))))
        start = input.length
    }

    val stylizedOutput = when (currentTag) {
        null -> output
        "b" -> output.map { it.stylize(BBCodeTag.Bold) }
        "i" -> output.map { it.stylize(BBCodeTag.Italic) }
        "quote" -> listOf(MessageQuote(quoteAuthor, output.collapseMessageTexts()))
        else -> {
            debug {
                Log.w(TAG, "Unknown/invalid tag $currentTag found near index $initialStart")
                Log.w(TAG, input)
            }
            buildList(output.size + 2) {
                // Enclose in tag
                add(MessageText(AnnotatedString("[$currentTag]")))
                addAll(output)
                add(MessageText(AnnotatedString("[/$currentTag]")))
            }
        }
    }

    return stylizedOutput to start
}

private fun parseQuoteHeader(input: String, initialStart: Int): Pair<MessageQuote.Author, Int>? {
    val match = quotePattern.matchAt(input, index = initialStart) ?: return null
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
private fun List<MessageData<*>>.collapseMessageTexts(): List<MessageData<*>> {
    // FAST PATH: Short comment, maybe with one quote or picture.
    if (this.size < 2) return this
        .filter { (it as? MessageText)?.text?.isNotEmpty() != false }
        .map { (it as? MessageText)?.trim() ?: it }
    // TD;DR accumulate MessageText until another MessageData is found, then continue with another accumulator)
    return this.asSequence()
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
        .toList()
}