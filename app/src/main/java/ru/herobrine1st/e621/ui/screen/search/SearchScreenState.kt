package ru.herobrine1st.e621.ui.screen.search

import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import ru.herobrine1st.e621.util.PostsSearchOptions
import ru.herobrine1st.e621.util.letApply

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
        PostsSearchOptions(ArrayList(tags), order, orderAscending, rating, favouritesOf
            .ifBlank { null })

    companion object {
        val Saver: Saver<SearchScreenState, Bundle> = Saver(
            save = { state ->
                state.letApply {
                    val bundle = Bundle()
                    bundle.putParcelable(
                        PostsSearchOptions::class.simpleName,
                        PostsSearchOptions(tags, order, orderAscending, rating, favouritesOf)
                    )
                    bundle.putBoolean(
                        SearchScreenState::openAddTagDialog.name,
                        openAddTagDialog
                    )
                    return@letApply bundle
                }
            },
            restore = { bundle ->
                return@Saver SearchScreenState(
                    bundle.getParcelable(PostsSearchOptions::class.simpleName)!!,
                    bundle.getBoolean(SearchScreenState::openAddTagDialog.name)
                )
            }
        )
    }
}