package ru.herobrine1st.e621.ui.dialog

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R

@Composable
fun DisclaimerDialog(
    title: String = stringResource(R.string.disclaimer),
    applyButtonEnabled: Boolean = true,
    onApply: () -> Unit,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ActionDialog(
        title = title,
        actions = {
            // Place button to the start so it can't be clicked intuitively
            TextButton(
                onClick = onApply,
                enabled = applyButtonEnabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text(stringResource(R.string.disclaimer_agree))
            }
            Spacer(Modifier.weight(1f))
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

@Composable
fun DisclaimerDialog(
    text: String,
    title: String = stringResource(R.string.disclaimer),
    applyButtonEnabled: Boolean = true,
    onApply: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    DisclaimerDialog(
        title = title,
        applyButtonEnabled = applyButtonEnabled,
        onApply = onApply,
        onDismissRequest = onDismissRequest,
        content = {
            Text(text)
        }
    )
}