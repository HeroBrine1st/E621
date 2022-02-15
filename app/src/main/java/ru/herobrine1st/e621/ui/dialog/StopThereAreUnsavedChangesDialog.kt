package ru.herobrine1st.e621.ui.dialog

import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R

@Composable
fun StopThereAreUnsavedChangesDialog(onClose: () -> Unit, onExit: () -> Unit) {
    ActionDialog(title = stringResource(R.string.unsaved_changes), actions = {
        TextButton(
            onClick = {onExit(); onClose()},
            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
        ) {
            Text(stringResource(R.string.leave_discard_changes))
        }
        TextButton(onClick = onClose) {
            Text(stringResource(R.string.cancel))
        }
    }, onDismissRequest = onClose) {
        Text(stringResource(R.string.unsaved_changes_message))
    }
}