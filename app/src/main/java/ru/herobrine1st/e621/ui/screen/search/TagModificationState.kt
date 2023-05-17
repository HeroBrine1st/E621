package ru.herobrine1st.e621.ui.screen.search

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface TagModificationState : Parcelable {
    @Parcelize
    object None : TagModificationState

    @Parcelize
    object AddingNew : TagModificationState

    @Parcelize
    class Editing(val index: Int) : TagModificationState
}