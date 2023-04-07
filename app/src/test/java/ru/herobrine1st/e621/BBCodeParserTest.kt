package ru.herobrine1st.e621

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.withStyle
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.herobrine1st.e621.api.*


class BBCodeParserTest {
    @Test
    fun testBoldSimple() {
        val res = parseBBCode("[b]Bold[/b]")
        assertEquals(1, res.size)
        assertEquals(
            AnnotatedString.Builder()
                .apply {
                    withStyle(BOLD) {
                        append("Bold")
                    }
                }.toAnnotatedString(),
            (res[0] as MessageText).text
        )

    }

    @Test
    fun testBoldMultiline() {
        val res = parseBBCode("Some text\n[b]Some another text[/b]")
        assertEquals(1, res.size)
        assertEquals(
            AnnotatedString.Builder()
                .apply {
                    append("Some text\n")
                    withStyle(BOLD) {
                        append("Some another text")
                    }
                }.toAnnotatedString(),
            (res[0] as MessageText).text
        )
    }

    @Test
    fun testItalicSimple() {
        val res = parseBBCode("[i]Italic[/i]")
        assertEquals(1, res.size)
        assertEquals(
            AnnotatedString.Builder()
                .apply {
                    withStyle(ITALIC) {
                        append("Italic")
                    }
                }.toAnnotatedString(),
            (res[0] as MessageText).text
        )
    }

    @Test
    fun testBoldPlusItalicSimple() {
        val res = parseBBCode("[b][i]Italic and bold[/i][/b]")
        assertEquals(1, res.size)
        assertEquals(
            AnnotatedString.Builder()
                .apply {
                    withStyle(BOLD) {
                        withStyle(ITALIC) {
                            append("Italic and bold")
                        }
                    }
                }.toAnnotatedString(),
            (res[0] as MessageText).text
        )
    }

    @Test
    fun testBoldPlusItalicComplicated() {
        val res = parseBBCode("some text [b][i]Italic and bold[/i][/b] [b]Bold[/b] [i]Italic[/i]")
        assertEquals(1, res.size)
        assertEquals(
            AnnotatedString.Builder()
                .apply {
                    append("some text ")
                    withStyle(BOLD) {
                        withStyle(ITALIC) {
                            append("Italic and bold")
                        }
                    }
                    append(" ")
                    withStyle(BOLD) {
                        append("Bold")
                    }
                    append(" ")
                    withStyle(ITALIC) {
                        append("Italic")
                    }
                }.toAnnotatedString(),
            (res[0] as MessageText).text
        )
    }

    // I assume there should be no nested quotes
    // UPD: now they are supported 🥳
    // Also this test.. ehh.. tests carriage return support - because e6 API uses \r\n as newlines
    @Test
    fun testQuoteSimple() {
        val res = parseBBCode("[quote]\"name\":/user/show/0 said:\r\nSome text\r\n[b]Some another text[/b]\r\n[/quote]")
        assertEquals(1, res.size)

        val quote = res[0] as MessageQuote
        assertEquals(1, quote.data.size)
        assertEquals(
            AnnotatedString.Builder()
                .apply {
                    append("Some text\r\n")
                    withStyle(BOLD) {
                        append("Some another text")
                    }
                }.toAnnotatedString(),
            (quote.data.first() as MessageText).text
        )
        assertEquals("name", quote.author?.userName)
        assertEquals(0, quote.author?.userId)
    }

    @Test
    fun testQuoteComplicated() {
        val res = parseBBCode("""
            [b]Bold text[/b] Normal text
            [quote]"name":/user/show/0 said:
            Some text
            [b]Some another text[/b]
            [/quote]
            [b]Bold text[/b] Normal text
            """.trimIndent())
        assertEquals(3, res.size)
        val text = res[0] as MessageText
        assertEquals(
            AnnotatedString.Builder()
                .apply {
                    withStyle(BOLD) {
                        append("Bold text")
                    }
                    append(" Normal text")
                }
                .toAnnotatedString(),
            text.text
        )

        val quote = res[1] as MessageQuote
        assertEquals(1, quote.data.size)
        assertEquals(
            AnnotatedString.Builder()
                .apply {
                    append("Some text\n")
                    withStyle(BOLD) {
                        append("Some another text")
                    }
                }.toAnnotatedString(),
            (quote.data[0] as MessageText).text
        )
        assertEquals("name", quote.author?.userName)
        assertEquals(0, quote.author?.userId)

        val text2 = res[2] as MessageText
        assertEquals(
            AnnotatedString.Builder()
                .apply {
                    withStyle(BOLD) {
                        append("Bold text")
                    }
                    append(" Normal text")
                }
                .toAnnotatedString(),
            text2.text
        )
    }
}