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
 * @param userName user who sent referred message
 * @param userId user who sent referred message
 * @param data part of text in referred message
 */
@Immutable
data class MessageQuote(
    val userName: String,
    val userId: Int,
    val data: List<MessageData<*>>
) : MessageData<MessageQuote> {
    override fun isEmpty(): Boolean = data.isEmpty()
    override fun getDescription(): Int = R.string.message_quote
    override fun stylize(tag: BBCodeTag): MessageQuote = this

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

// [quote]"name":/user/show/0 said:
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

    // name, id
    val quoteData: Pair<String, Int>? = currentTag
        ?.takeIf { it == "quote" }
        ?.let { parseQuoteHeader(input, start) }
        ?.let { res ->
            start = res.third
            res.first to res.second
        }

    fun stylize() = when {
        currentTag == null -> output
        currentTag == "b" -> output.map { it.stylize(BBCodeTag.Bold) }
        currentTag == "i" -> output.map { it.stylize(BBCodeTag.Italic) }
        currentTag == "quote" && quoteData != null -> listOf(
            MessageQuote(quoteData.first, quoteData.second, output.collapseMessageTexts())
        )
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



    while (true) {
        val match: MatchResult = pattern.find(input, startIndex = start) ?: break
        if (match.range.first > start) { // Include text ([tag] -> recurse -> include_this_text[\tag])
            output.add(MessageText(AnnotatedString(input.substring(start, match.range.first))))
        }
        when (val tag = match.groupValues[2]) {
            "" -> {}
            // Tag closed, stylize and leave
            currentTag -> return stylize() to match.range.last + 1
            // Invalid closing tag
            // Abort, stylize and leave
            else -> {
                debug {
                    Log.d(TAG, "Invalid closing tag found at indexes ${match.range}")
                    Log.d(TAG, input)
                }
                return stylize() + listOf(MessageText(AnnotatedString("[/$tag]"))) to match.range.last + 1
            }
        }
        when (val tag = match.groupValues[1]) {
            "" -> {}
            else -> {
                output += parseBBCodeInternal(input, tag, match.range.last + 1).also {
                    start = it.second
                }.first
            }
        }

        if (start >= input.length) break
    }
    output.add(MessageText(AnnotatedString(input.substring(start))))
    return stylize() to input.length
}

private fun parseQuoteHeader(input: String, initialStart: Int): Triple<String, Int, Int>? {
    val match = quotePattern.matchAt(input, index = initialStart) ?: return null
    return Triple(
        match.groupValues[1].ifEmpty { match.groupValues[3] }, match.groupValues[2].ifEmpty { "-1" }.toInt(),
        match.range.last + 1
    )
}

private fun List<MessageData<*>>.collapseMessageTexts(): List<MessageData<*>> {
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