package ru.herobrine1st.e621.ui.dialog

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R


@Composable
fun ContentDialog(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
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
        content()
    }
}