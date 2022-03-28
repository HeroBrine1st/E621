package ru.herobrine1st.e621.api.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
data class Relationships(
    val parentId: Int,
    val hasChildren: Boolean,
    val hasActiveChildren: Boolean,
    val children: List<Int>
) : Parcelable