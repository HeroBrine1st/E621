package ru.herobrine1st.e621.ui.component.post

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.ui.screen.posts.component.InvalidPost

@Composable
fun PostMediaContainer(
    file: NormalizedFile,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    when {
        file.type.isVideo -> PostVideo(
            file,
            modifier = modifier
                .fillMaxWidth()
                .zIndex(1f)
        )
        file.type.isImage -> PostImage(
            file = file,
            contentDescription = contentDescription,
            modifier = modifier.fillMaxWidth()
        )
        else -> InvalidPost(
            text = stringResource(
                R.string.unsupported_post_type,
                file.type.extension
            )
        )
    }
}