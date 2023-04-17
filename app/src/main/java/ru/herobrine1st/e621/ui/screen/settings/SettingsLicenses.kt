package ru.herobrine1st.e621.ui.screen.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffold
import ru.herobrine1st.e621.ui.component.scaffold.MainScaffoldState

@Composable
fun SettingsLicenses(mainScaffoldState: MainScaffoldState) {
    MainScaffold(
        state = mainScaffoldState,
        title = { Text(stringResource(R.string.oss_licenses)) },
    ) {
        LibrariesContainer(Modifier.fillMaxSize())
    }
}