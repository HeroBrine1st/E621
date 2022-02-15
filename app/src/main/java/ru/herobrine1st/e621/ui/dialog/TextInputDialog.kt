package ru.herobrine1st.e621.ui.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R

@Composable
fun TextInputDialog(
    title: String,
    initialText: String = "",
    submitButtonText: String = stringResource(R.string.submit),
    textFieldLabel: String = "",
    onClose: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    ActionDialog(
        title = title,
        actions = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close))
            }
            TextButton(onClick = { onSubmit(text); onClose() }) {
                Text(submitButtonText)
            }
        },
        onDismissRequest = onClose
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(textFieldLabel) },
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}