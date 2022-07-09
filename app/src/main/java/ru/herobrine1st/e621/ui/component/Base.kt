package ru.herobrine1st.e621.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val BASE_PADDING_HORIZONTAL = 8.dp

@Composable
fun Base(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = BASE_PADDING_HORIZONTAL)
            .fillMaxHeight()
            .fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

