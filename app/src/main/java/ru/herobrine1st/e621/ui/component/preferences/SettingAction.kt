package ru.herobrine1st.e621.ui.component.preferences

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Setting with only action part
 */
@Composable
fun SettingAction(
    checked: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    action: @Composable (BoxScope.() -> Unit)
) {
    Surface {
        Row(
            modifier = modifier
                .toggleable(
                    value = checked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                    onValueChange = { onCheckedChange(it) },
                )
                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            SettingIcon(icon)
            SettingTexts(title, subtitle, modifier = Modifier.weight(1f))
            SettingActionBox(content = action)
        }
    }
}