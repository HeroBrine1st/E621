package ru.herobrine1st.e621.ui.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R

@Composable
@Preview
fun SettingsAbout() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {} // "padding"
        item {
            Card(Modifier.padding(horizontal = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        Image(
                            painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(128.dp)
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.subtitle1
                        )
                        Spacer(modifier = Modifier.size(2.dp))
                        Text(
                            stringResource(
                                R.string.app_description,
                                BuildConfig.DEEP_LINK_BASE_URL
                            ),
                            Modifier.alpha(ContentAlpha.medium),
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.non_affiliation_disclaimer))
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    val uriHandler = LocalUriHandler.current
                    val licenseUrl = stringResource(R.string.license_url)
                    Text(stringResource(R.string.license_word), style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.license_brief))
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = { uriHandler.openUri(licenseUrl) }) {
                        Text(stringResource(R.string.license_name))
                        Icon(
                            Icons.Default.OpenInBrowser,
                            contentDescription = stringResource(R.string.open_in_browser)
                        )
                    }
                }
            }
        }
        item {}
    }
}