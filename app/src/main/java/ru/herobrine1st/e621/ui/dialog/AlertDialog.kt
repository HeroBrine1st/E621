package ru.herobrine1st.e621.ui.dialog

import android.app.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
    ActionDialog(
        title = title,
        actions = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(R.string.close))
            }
        },
        onDismissRequest = onDismissRequest
    ) {
        Text(text)
    }
}