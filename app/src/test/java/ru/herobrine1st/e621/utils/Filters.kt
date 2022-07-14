package ru.herobrine1st.e621.utils

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.text.AnnotatedString

/*
 * hasText - checks substring (so, for example, matcher "Text + EditableText contains 'text' (ignoreCase: false) as substring"
   matches node with EditableText = [blah text blah])
 * hasTextExactly - checks strictly both Text and EditableText (so, for example, if TextField has
   label "Text" and input "input", assert(hasTextExactly("input")) will find that it has "Text" and fail)
 => cannot check only EditableText for equality with standard tools
 */
fun editableTextEquals(text: String) = SemanticsMatcher("EditableText = [$text]") {
    it.config.getOrNull(SemanticsProperties.EditableText) == AnnotatedString(text)
}

fun editableTextMatches(predicate: AnnotatedString.() -> Boolean) = SemanticsMatcher("EditableText matcher]") {
    it.config.getOrNull(SemanticsProperties.EditableText)?.predicate() ?: false
}