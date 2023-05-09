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

package ru.herobrine1st.e621.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.MessageData
import ru.herobrine1st.e621.api.MessageQuote
import ru.herobrine1st.e621.api.MessageText
import ru.herobrine1st.e621.api.WIKI_PAGE_STRING_ANNOTATION_TAG
import ru.herobrine1st.e621.api.parseBBCode

@Composable
fun RenderBB(text: String, onWikiLinkClick: ((String) -> Unit)? = null) {
    val parsed = remember(text) { parseBBCode(text, handleLinks = onWikiLinkClick != null) }
    RenderBB(parsed)
}

@Composable
fun RenderBB(data: List<MessageData<*>>, onWikiLinkClick: ((String) -> Unit)? = null) {
    Column {
        data.forEach {
            RenderBB(it, onWikiLinkClick = onWikiLinkClick)
        }
    }
}

@Composable
fun RenderBB(data: MessageData<*>, onWikiLinkClick: ((String) -> Unit)? = null) {
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
                val text = data.text
                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                Text(
                    text,
                    onTextLayout = {
                        layoutResult = it
                    },
                    modifier = Modifier
                        // TODO hover indication via drawBehind
                        .pointerInput(onWikiLinkClick, text) {
                            detectTapGestures { pos ->
                                val layoutResultNonNull =
                                    layoutResult ?: return@detectTapGestures
                                val index = layoutResultNonNull.getOffsetForPosition(pos)
                                text
                                    .getStringAnnotations(
                                        tag = WIKI_PAGE_STRING_ANNOTATION_TAG,
                                        start = index,
                                        end = index
                                    )
                                    .firstOrNull()
                                    ?.let {
                                        onWikiLinkClick(it.item)
                                    }
                            }
                        }
                )
            } else {
                Text(data.text)
            }
        }
    }
}