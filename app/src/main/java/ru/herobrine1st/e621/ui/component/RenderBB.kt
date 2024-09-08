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

package ru.herobrine1st.e621.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.MessageData
import ru.herobrine1st.e621.api.MessageQuote
import ru.herobrine1st.e621.api.MessageText
import ru.herobrine1st.e621.api.WIKI_PAGE_STRING_ANNOTATION_TAG
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.api.parseBBCode
import ru.herobrine1st.e621.ui.theme.LightBlue
import ru.herobrine1st.e621.ui.theme.isLight

@Composable
fun RenderBB(text: String, onWikiLinkClick: ((Tag) -> Unit)? = null) {
    val parsed = remember(text) { parseBBCode(text) }
    RenderBB(parsed, onWikiLinkClick = onWikiLinkClick)
}

@Composable
fun RenderBB(data: List<MessageData>, onWikiLinkClick: ((Tag) -> Unit)? = null) {
    Column {
        data.forEach {
            RenderBB(it, onWikiLinkClick = onWikiLinkClick)
        }
    }
}

@Composable
fun RenderBB(data: MessageData, onWikiLinkClick: ((Tag) -> Unit)? = null) {
    when (data) {
        is MessageQuote -> {
            data.author?.let {
                Text(stringResource(R.string.quote_comments, it.userName))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                Box(
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(4.dp))
                RenderBB(
                    data = data.data
                )
            }
        }

        is MessageText -> {
            if (onWikiLinkClick != null) {
                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                var linkPath by remember { mutableStateOf<Path?>(null) }
                val colorScheme = MaterialTheme.colorScheme
                val isLight by remember { derivedStateOf { colorScheme.isLight } }

                val text by remember(data.text) {
                    derivedStateOf {
                        val linkSpanStyle = SpanStyle(
                            color = if (isLight) Color.Blue else Color.LightBlue,
                            textDecoration = TextDecoration.Underline
                        )
                        AnnotatedString.Builder(data.text).apply {
                            data.text.getStringAnnotations(
                                WIKI_PAGE_STRING_ANNOTATION_TAG,
                                0,
                                data.text.length
                            ).forEach {
                                addStyle(linkSpanStyle, it.start, it.end)
                            }
                        }.toAnnotatedString()
                    }
                }

                Text(
                    text,
                    onTextLayout = {
                        layoutResult = it
                    },
                    modifier = Modifier
                        // TODO new APIs, they probably can replace this code
                        // https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.7.0
                        // The ClickableText has been deprecated. To add clickable links to your text, use BasicText with the new LinkAnnotation annotation in your AnnotatedString. LinkAnnotation allows for custom styling based on link state (e.g. focused, hovered).
                        .pointerInput(onWikiLinkClick) {
                            // Reason to not use detectTapGestures: SelectionContainer does not get tap gestures
                            awaitEachGesture {
                                val layoutResultNotNull =
                                    layoutResult ?: return@awaitEachGesture
                                val down = awaitFirstDown()

                                // If there's no link at touch position, do nothing
                                val index =
                                    layoutResultNotNull.getOffsetForPosition(down.position)
                                val annotations = text.getStringAnnotations(
                                    tag = WIKI_PAGE_STRING_ANNOTATION_TAG,
                                    start = index,
                                    end = index
                                )
                                val annotation =
                                    annotations.firstOrNull() ?: return@awaitEachGesture

                                // Consume and wait for up
                                down.consume()
                                linkPath = layoutResultNotNull.getPathForRange(
                                    annotation.start,
                                    annotation.end
                                )
                                // TODO handle position change and move indication accordingly
                                // also it somehow knows size of annotation (not only one word!
                                // even spaced link is working correctly)
                                // and cancels it if touch is out of annotated text
                                // idk how does it know it
                                // inspection of source code led me to NodeCoordinator's measuredSize
                                // So, I guess, every word/annotated fragment is rendered by one
                                // but modifier is applied to the whole layout, not word-by-word
                                // That's a contradiction
                                // FIXME (or that would be a feature, I don't know yet)
                                //       selection is possible if you press long enough
                                val up = waitForUpOrCancellation()

                                linkPath = null
                                // Cancelled or just out of bounds of annotation (described above)
                                if (up == null) return@awaitEachGesture
                                // Check if up event was on the same annotation
                                val indexFinal =
                                    layoutResultNotNull.getOffsetForPosition(up.position)
                                val annotationsFinal = text.getStringAnnotations(
                                    tag = WIKI_PAGE_STRING_ANNOTATION_TAG,
                                    start = indexFinal,
                                    end = indexFinal
                                )
                                val annotationFinal =
                                    annotationsFinal.firstOrNull() ?: return@awaitEachGesture
                                if (annotation != annotationFinal) return@awaitEachGesture

                                onWikiLinkClick(Tag(annotationFinal.item))
                            }
                        }
                        .drawBehind {
                            linkPath?.let { path ->
                                val color =
                                    if (isLight) Color.Blue.copy(alpha = 0.25f)
                                    else Color.LightBlue.copy(alpha = 0.5f)
                                drawPath(path, color = color)
                            }
                        }
                )
            } else {
                Text(data.text)
            }
        }
    }
}