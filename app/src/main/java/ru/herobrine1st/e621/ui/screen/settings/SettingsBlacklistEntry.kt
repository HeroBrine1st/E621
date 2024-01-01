package ru.herobrine1st.e621.ui.screen.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.navigation.component.settings.SettingsBlacklistEntryComponent
import ru.herobrine1st.e621.ui.component.scaffold.ActionBarMenu
import ru.herobrine1st.e621.ui.component.scaffold.ScreenSharedState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBlacklistEntry(
    screenSharedState: ScreenSharedState,
    component: SettingsBlacklistEntryComponent
) {
    var applying by remember { mutableStateOf(false) }
    val backdropFactor by animateFloatAsState(if (!applying) 0f else 1f, label = "Backdrop factor animation")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.screen_settings_blacklist_entry))
                },
                actions = {
                    ActionBarMenu(
                        onNavigateToSettings = screenSharedState.goToSettings,
                        onOpenBlacklistDialog = screenSharedState.openBlacklistDialog
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = screenSharedState.snackbarHostState)
        }
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
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
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        applying = true
                        component.apply {
                            applying = false
                        }
                    },
                    enabled = !applying && component.query.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Crossfade(component.id, label = "Button label crossfade") { id ->
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