package ru.herobrine1st.e621.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

const val BASE_WIDTH = 0.95f

@Composable
fun Base(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .fillMaxWidth(BASE_WIDTH),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

