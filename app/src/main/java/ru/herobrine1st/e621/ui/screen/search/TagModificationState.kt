package ru.herobrine1st.e621.ui.screen.search

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
sealed interface TagModificationState {

    @Serializable
    data object None : TagModificationState

    @Serializable
    data object AddingNew : TagModificationState

    @Serializable
    class Editing(val index: Int) : TagModificationState

    object Saver: androidx.compose.runtime.saveable.Saver<MutableState<TagModificationState>, String> {
        override fun restore(value: String): MutableState<TagModificationState> {
            return mutableStateOf(Json.decodeFromString(value))
        }

        override fun SaverScope.save(value: MutableState<TagModificationState>): String {
            return Json.encodeToString(value.value)
        }


    }
}