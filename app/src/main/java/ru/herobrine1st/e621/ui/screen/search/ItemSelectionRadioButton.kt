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

package ru.herobrine1st.e621.ui.screen.search

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ru.herobrine1st.e621.ui.theme.disabledText

@Composable
fun ItemSelectionRadioButton(
    selected: Boolean,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                enabled = enabled,
            )
            .fillMaxWidth()
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary)
        )
        Text(
            text = text,
            color = if (enabled) Color.Unspecified else MaterialTheme.colors.disabledText
        )
    }
}