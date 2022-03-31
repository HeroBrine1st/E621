package ru.herobrine1st.e621.ui.component.preferences

import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxColors
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SettingLinkWithCheckbox(
    checked: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(
        checkedColor = MaterialTheme.colors.primary,
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
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = checkboxColors
        )
    }
}