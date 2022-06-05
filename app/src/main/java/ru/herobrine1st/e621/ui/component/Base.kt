package ru.herobrine1st.e621.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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

@Composable
fun LazyBase(
    lazyListState: LazyListState = rememberLazyListState(),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(BASE_WIDTH),
            state = lazyListState,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}