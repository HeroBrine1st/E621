package ru.herobrine1st.e621.api

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.util.regex.Pattern

val BOLD = SpanStyle(
    fontWeight = FontWeight.Bold
)

val ITALIC = SpanStyle(
    fontStyle = FontStyle.Italic
)

// First alternative: (fast way) match any text until "[" is found
// Second alternative: match tag
// Third alternative: (slow way) match any text (fallback in case of invalid tag)
val pattern: Pattern = Pattern.compile(
    "[^\\[]+|\\[(\\w+?)](.+?)\\[/\\1]|.+?",
    Pattern.MULTILINE or Pattern.DOTALL
)
val quotePattern: Pattern = Pattern.compile(
    "\"([^\"]+)\":/user/show/(\\d+) said:(.+)",
    Pattern.MULTILINE or Pattern.DOTALL
)

@Immutable
interface MessageData {
    val text: AnnotatedString
}

/**
 * Regular text in message
 * @param text Part of text in message
 */
@Immutable
data class MessageText(
    override val text: AnnotatedString,
) : MessageData

/**
 * Quote in message
 * @param userName user who sent referred message
 * @param userId user who sent referred message
 * @param text part of text in referred message
 */
@Immutable
data class MessageQuote(
    val userName: String,
    val userId: Int,
    override val text: AnnotatedString
) : MessageData

@Stable
fun parseBBCode(input: String): List<MessageData> {
    val matcher = pattern.matcher(input)
    var builder = AnnotatedString.Builder()
    val res = mutableListOf<MessageData>()

    fun fold() {
        res.add(
            MessageText(
                text = builder
                    .toAnnotatedString()
                    .trim('\n', '\r') as AnnotatedString
            )
        )
        builder = AnnotatedString.Builder()
    }

    while (matcher.find()) {
        if (matcher.group(1) != null) { // Found tag
            val tag = matcher.group(1)!!
            val inner = matcher.group(2)!!
            when (tag) {
                "quote" -> {
                    val match = quotePattern.matcher(inner)
                    if (match.matches()) {
                        fold()
                        res.add(
                            MessageQuote(
                                userName = match.group(1)!!,
                                userId = match.group(2)!!.toInt(),
                                text = parseBBCodeInternal(
                                    match.group(3)!!
                                        // [quote]author...\r\nQuote text\r\n[/quote]
                                        .trim('\n', '\r')
                                ),
                            )
                        )
                    } else { // Fallback
                        stylizeText(tag, inner, builder)
                    }
                }
                else -> stylizeText(tag, inner, builder)
            }
        } else { // No markup found
            builder.append(matcher.group())
        }
    }
    fold()
    return res.filter { it.text.isNotEmpty() }
}

private fun parseBBCodeInternal(input: String): AnnotatedString {
    val matcher = pattern.matcher(input)
    val builder = AnnotatedString.Builder()
    while (matcher.find()) {
        if (matcher.group(1) != null) {
            stylizeText(matcher.group(1)!!, matcher.group(2)!!, builder)
        } else {
            builder.append(matcher.group())
        }
    }
    return builder.toAnnotatedString()
}

private fun stylizeText(tag: String, inner: String, builder: AnnotatedString.Builder) {
    when (tag) {
        "b" -> builder.withStyle(BOLD) {
            append(parseBBCodeInternal(inner))
        }
        "i" -> builder.withStyle(ITALIC) {
            append(parseBBCodeInternal(inner))
        }
        else -> {
            Log.w("BB Code Parser", "Tag $tag isn't supported by primitive handler")
            builder.append("[$tag]")
            builder.append(parseBBCodeInternal(inner))
            builder.append("[/$tag]")
        }
    }
}
