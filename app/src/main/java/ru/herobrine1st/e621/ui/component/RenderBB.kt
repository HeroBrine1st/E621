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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.MessageData
import ru.herobrine1st.e621.api.MessageQuote
import ru.herobrine1st.e621.api.MessageText
import ru.herobrine1st.e621.api.parseBBCode
import ru.herobrine1st.e621.ui.theme.disabledText

@Composable
fun RenderBB(text: String) {
    val parsed = remember(text) { parseBBCode(text) }
    RenderBB(parsed)
}

@Composable
fun RenderBB(data: List<MessageData<*>>) {
    data.forEach {
        RenderBB(it)
    }
}

@Composable
fun RenderBB(data: MessageData<*>) {
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
                        .background(MaterialTheme.colors.disabledText)
                )
                Spacer(Modifier.width(4.dp))
                RenderBB(
                    data = data.data
                )
            }
        }
        is MessageText -> Text(data.text)
    }
}