package ru.herobrine1st.e621.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private val SettingHeight = 64.dp
private val DividerHeight = 40.dp

@Composable
fun SettingIcon(
    icon: ImageVector?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(SettingHeight), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            if (icon != null) {
                Icon(icon, contentDescription = null)
            }
        }
    }
}

@Composable
fun SettingTexts(
    title: String,
    subtitle: String?,
) {
    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.subtitle1)
        if (subtitle != null) {
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                subtitle,
                Modifier.alpha(ContentAlpha.medium),
                style = MaterialTheme.typography.caption
            )
        }
    }
}

@Composable
fun SettingActionBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier.size(SettingHeight),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Switch with clickable and toggleable parts
 */
@Composable
fun SettingLinkSwitch(
    checked: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
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
                        checked,
                        remember { MutableInteractionSource() },
                        null,
                        onValueChange = { onCheckedChange(it) },
                    )
            ) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colors.primary,
                        uncheckedThumbColor = MaterialTheme.colors.onSurface
                    )
                )
            }
        }
    }
}

/**
 * Clickable setting
 */
@Composable
fun SettingLink(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface {
        Row(
            modifier = modifier
                .clickable(onClick = onClick)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingIcon(icon)
            SettingTexts(title, subtitle)
        }
    }
}