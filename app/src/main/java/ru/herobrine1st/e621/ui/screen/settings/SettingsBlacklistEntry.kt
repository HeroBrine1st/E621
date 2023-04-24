package ru.herobrine1st.e621.ui.screen.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.settings.SettingsBlacklistEntryComponent
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffold
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffoldState

@Composable
fun SettingsBlacklistEntry(
    mainScaffoldState: MainScaffoldState,
    component: SettingsBlacklistEntryComponent
) {
    var applying by remember { mutableStateOf(false) }
    val backdropFactor by animateFloatAsState(if (!applying) 0f else 1f)

    MainScaffold(
        state = mainScaffoldState,
        title = { Text(stringResource(R.string.screen_settings_blacklist_entry)) }
    ) {
        Box {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                OutlinedTextField(
                    value = component.query,
                    onValueChange = {
                        component.query = it
                    },
                    label = {
                        Text(stringResource(R.string.tag_combination))
                    },
                    singleLine = true,
                    enabled = !applying,
                    trailingIcon = {
                        Switch(
                            checked = component.enabled,
                            onCheckedChange = {
                                component.enabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.primary,
                                uncheckedThumbColor = MaterialTheme.colors.onSurface
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        applying = true
                        component.apply {
                            applying = false
                        }
                    },
                    enabled = !applying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Crossfade(component.id) { id ->
                        Text(
                            when (id) {
                                0L -> stringResource(R.string.add)
                                else -> stringResource(R.string.apply)
                            }
                        )
                    }
                }
            }

            if (backdropFactor > 0f) Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(
                            alpha = backdropFactor * 0.4f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    Modifier.alpha(backdropFactor)
                )
            }
        }
    }
}