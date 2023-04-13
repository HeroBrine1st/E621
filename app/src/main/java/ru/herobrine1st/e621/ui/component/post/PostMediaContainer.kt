package ru.herobrine1st.e621.ui.component.post

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.NormalizedFile

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
                .zIndex(1f) // TODO idk what is it for
        )
        file.type.isImage -> PostImage(
            file = file,
            contentDescription = contentDescription,
            modifier = modifier
        )
        else -> InvalidPost(
            text = stringResource(
                R.string.unsupported_post_type,
                file.type.extension
            ),
            modifier = modifier
        )
    }
}