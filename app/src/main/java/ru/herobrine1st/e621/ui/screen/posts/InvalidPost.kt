package ru.herobrine1st.e621.ui.screen.posts

import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

@Composable
fun InvalidPost(text: String) {
    Box(contentAlignment = Alignment.TopCenter) {
        Text(text)
    }
}