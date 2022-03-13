package ru.herobrine1st.e621.ui.screen.search

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import ru.herobrine1st.e621.api.Rating
import ru.herobrine1st.e621.util.PostsSearchOptions

class SearchScreenState(
    initialPostsSearchOptions: PostsSearchOptions,
    openDialog: Boolean = false
) {
    val tags = mutableStateListOf<String>().also { it.addAll(initialPostsSearchOptions.tags) }
    var order by mutableStateOf(initialPostsSearchOptions.order)
    var orderAscending by mutableStateOf(initialPostsSearchOptions.orderAscending)
    var rating = mutableStateListOf<Rating>().also { it.addAll(initialPostsSearchOptions.rating) }
    var favouritesOf by mutableStateOf(initialPostsSearchOptions.favouritesOf ?: "")

    var openDialog by mutableStateOf(openDialog)

    fun makeSearchOptions(): PostsSearchOptions =
        PostsSearchOptions(ArrayList(tags), order, orderAscending, rating, favouritesOf.ifBlank { null })

    companion object {
        val Saver: Saver<SearchScreenState, Bundle> = Saver(
            save = { state ->
                val bundle = Bundle()
                bundle.putString(
                    "tags",
                    state.tags.joinToString(",")
                ) // Can't use putStringArrayList because restore constructor used with Navigation
                bundle.putString("order", state.order.name)
                bundle.putBoolean("ascending", state.orderAscending)
                bundle.putString("rating", state.rating.joinToString(",") { it.name })
                bundle.putBoolean("openDialog", state.openDialog)
                bundle.putString("fav", state.favouritesOf)
                return@Saver bundle
            },
            restore = { bundle ->
                val searchOptions = PostsSearchOptions(bundle)
                return@Saver SearchScreenState(searchOptions, bundle.getBoolean("openDialog"))
            }
        )
    }
}