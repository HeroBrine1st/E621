package ru.herobrine1st.e621.ui.screen.search

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.util.getParcelableCompat

class SearchScreenState(
    initialPostsSearchOptions: PostsSearchOptions,
    openAddTagDialog: Boolean = false
) {
    val tags = initialPostsSearchOptions.tags.toMutableStateList()
    var order by mutableStateOf(initialPostsSearchOptions.order)
    var orderAscending by mutableStateOf(initialPostsSearchOptions.orderAscending)
    var rating = initialPostsSearchOptions.rating.toMutableStateList()
    var favouritesOf by mutableStateOf(initialPostsSearchOptions.favouritesOf ?: "")

    var openAddTagDialog by mutableStateOf(openAddTagDialog)

    fun makeSearchOptions(): PostsSearchOptions =
        PostsSearchOptions(tags.toList(), order, orderAscending, rating.toList(), favouritesOf
            .ifBlank { null })

    companion object {
        val Saver: Saver<SearchScreenState, Bundle> = Saver(
            save = { state ->
                with(state) {
                    val bundle = Bundle()
                    bundle.putParcelable(
                        PostsSearchOptions::class.simpleName,
                        makeSearchOptions()
                    )
                    bundle.putBoolean(
                        SearchScreenState::openAddTagDialog.name,
                        openAddTagDialog
                    )
                    return@Saver bundle
                }
            },
            restore = { bundle ->
                return@Saver SearchScreenState(
                    bundle.getParcelableCompat(PostsSearchOptions::class.simpleName)!!,
                    bundle.getBoolean(SearchScreenState::openAddTagDialog.name)
                )
            }
        )
    }
}