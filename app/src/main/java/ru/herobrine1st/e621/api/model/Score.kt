package ru.herobrine1st.e621.api.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class Score(
    val up: Int,
    val down: Int,
    val total: Int
) : Parcelable