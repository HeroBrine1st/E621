package ru.herobrine1st.e621.ui.component.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Setting with clickable and action parts
 */
@Composable
fun SettingLinkWithAction(
    checked: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    action: @Composable (BoxScope.() -> Unit)
) {
    Surface {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = modifier
                    .clickable(onClick = onClick)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingIcon(icon)
                SettingTexts(title, subtitle)
            }
            Divider(
                modifier = Modifier
                    .height(DividerHeight)
                    .width(1.dp)
            )
            SettingActionBox(
                modifier = Modifier
                    .toggleable(
                        value = checked,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        onValueChange = { onCheckedChange(it) },
                    ),
                content = action
            )
        }
    }
}