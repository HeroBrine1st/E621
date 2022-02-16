package ru.herobrine1st.e621.ui.theme

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color

val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)

val statusBarDarkModeColor = Color(0xFF222222)
val ActionBarIconColor = Color.White

val Colors.statusBar get() = if(this.isLight) this.primary else statusBarDarkModeColor
val Colors.disabledText get() = if(this.isLight) Color.LightGray else Color.Gray