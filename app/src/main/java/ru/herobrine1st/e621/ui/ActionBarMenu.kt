package ru.herobrine1st.e621.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.screen.Screen
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor

@Composable
fun MenuAction(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Icon(icon, null, modifier = Modifier.padding(end = 8.dp))
        Text(text)
    }
}

@Composable
fun ActionBarMenu(
    navController: NavController,
    onOpenBlacklistDialog: () -> Unit
) {
    var openMenu by remember { mutableStateOf(false) }

    IconButton(onClick = { openMenu = !openMenu }) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.appbar_morevert),
            tint = ActionBarIconColor
        )
    }

    DropdownMenu(
        expanded = openMenu,
        onDismissRequest = { openMenu = false }
    ) {
        MenuAction(Icons.Outlined.Block, stringResource(R.string.blacklist)) {
            openMenu = false
            onOpenBlacklistDialog()
        }
        MenuAction(Icons.Outlined.Settings, stringResource(R.string.settings)) {
            openMenu = false
            if (navController.backQueue.any { it.destination.route == Screen.Settings.route }) return@MenuAction
            navController.navigate(Screen.Settings.route) {
                launchSingleTop = true
            }
        }
    }
}