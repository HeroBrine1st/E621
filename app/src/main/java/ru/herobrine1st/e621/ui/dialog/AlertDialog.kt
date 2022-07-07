package ru.herobrine1st.e621.ui.dialog

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R


// There is one but I prefer mine
@Composable
fun AlertDialog(
    text: String,
    title: String = stringResource(R.string.warning),
    onDismissRequest: () -> Unit
) {
    ContentDialog(onDismissRequest = onDismissRequest, title = title) {
        Text(text)
    }
}