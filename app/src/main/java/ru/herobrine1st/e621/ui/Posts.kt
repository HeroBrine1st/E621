package ru.herobrine1st.e621.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun Posts(navController: NavController, query: String) {
    Text(query)
}