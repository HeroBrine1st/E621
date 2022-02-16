package ru.herobrine1st.e621.ui.screen.search

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import ru.herobrine1st.e621.api.Rating
import ru.herobrine1st.e621.util.SearchOptions

class SearchScreenState(
    initialSearchOptions: SearchOptions,
    openDialog: Boolean = false
) {
    val tags = mutableStateListOf<String>().also { it.addAll(initialSearchOptions.tags) }
    var order by mutableStateOf(initialSearchOptions.order)
    var orderAscending by mutableStateOf(initialSearchOptions.orderAscending)
    var rating = mutableStateListOf<Rating>().also { it.addAll(initialSearchOptions.rating) }

    var openDialog by mutableStateOf(openDialog)

    fun makeSearchOptions(): SearchOptions =
        SearchOptions(ArrayList(tags), order, orderAscending, rating)

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
                return@Saver bundle
            },
            restore = { bundle ->
                val searchOptions = SearchOptions(bundle)
                return@Saver SearchScreenState(searchOptions, bundle.getBoolean("openDialog"))
            }
        )
    }
}