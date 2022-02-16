package ru.herobrine1st.e621.ui.screen.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.api.Order

@Composable
fun OrderItem(item: Order, selected: Boolean, onClick: () -> Unit) {
    key(item.apiName) {
        ItemSelectionRadioButton(
            selected = selected,
            text = stringResource(item.descriptionId),
            onClick = onClick
        )
    }
}