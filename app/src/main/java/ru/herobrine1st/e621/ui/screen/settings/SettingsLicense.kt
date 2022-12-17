package ru.herobrine1st.e621.ui.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.util.readRawResourceString

@Composable
@Preview
fun SettingsLicense() {
    Box(
        Modifier
            .padding(horizontal=8.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            readRawResourceString(R.raw.copying),
            fontFamily = FontFamily.Monospace
        )
    }
}