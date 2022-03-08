package ru.herobrine1st.e621.ui.screen.posts

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.api.LocalAPI
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.R

private val TAG = "Post Screen"

@Composable
fun Post(applicationViewModel: ApplicationViewModel, id: Int, scrollToComments: Boolean) {
    val api = LocalAPI.current
    var error by remember { mutableStateOf(false) }
    val post by produceState<Post?>(initialValue = null) {
        try {
            value = withContext(Dispatchers.IO) {
                api.getPost(id)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to get post", t)
            error = true
            applicationViewModel.addSnackbarMessage(
                R.string.network_error,
                SnackbarDuration.Indefinite
            )
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (post == null) {
            if (!error) { // => loading
                CircularProgressIndicator()
            } else {
                Text("Error") // TODO i18n
            }
            return@Column
        }
        PostImagePreview(post = post!!, null)
        Text("TODO")
        // TODO comments
        // TODO tags
    }
}