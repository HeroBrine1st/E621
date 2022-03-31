package ru.herobrine1st.e621.ui.component.preferences

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

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