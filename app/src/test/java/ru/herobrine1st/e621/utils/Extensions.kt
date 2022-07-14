package ru.herobrine1st.e621.utils

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.text.AnnotatedString

fun SemanticsNodeInteraction.assertEditableTextEquals(
    text: String
) = assert(editableTextEquals(text))

fun SemanticsNodeInteraction.assertEditableTextMatches(predicate: AnnotatedString.() -> Boolean) =
    assert(editableTextMatches(predicate))