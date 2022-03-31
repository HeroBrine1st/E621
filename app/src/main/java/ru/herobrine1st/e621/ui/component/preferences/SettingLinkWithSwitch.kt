package ru.herobrine1st.e621.ui.component.preferences

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchColors
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Switch with clickable and toggleable parts
 */
@Composable
fun SettingLinkWithSwitch(
    checked: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    switchColors: SwitchColors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colors.primary,
        uncheckedThumbColor = MaterialTheme.colors.onSurface
    ),
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    SettingLinkWithAction(
        checked = checked,
        title = title,
        modifier = modifier,
        icon = icon,
        subtitle = subtitle,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        onClick = onClick
    ) {
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = switchColors
        )
    }
}