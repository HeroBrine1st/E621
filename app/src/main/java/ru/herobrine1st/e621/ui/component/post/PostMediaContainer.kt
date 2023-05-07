package ru.herobrine1st.e621.ui.component.post

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.navigation.component.VideoPlayerComponent

@Composable
fun PostMediaContainer(
    file: NormalizedFile,
    contentDescription: String?,
    getVideoPlayerComponent: () -> VideoPlayerComponent,
    modifier: Modifier = Modifier,
    post: Post? = null
) {
    when {
        file.type.isVideo -> PostVideo(
            getVideoPlayerComponent(),
            file,
            modifier = modifier
        )
        file.type.isImage -> PostImage(
            file = file,
            contentDescription = contentDescription,
            modifier = modifier,
            actualPostFileType = post?.file?.type
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