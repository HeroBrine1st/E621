package ru.herobrine1st.e621.ui.screen.posts

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.AuthState
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.model.Post

@Composable
fun PostActionsRow(
    post: Post,
    applicationViewModel: ApplicationViewModel,
    openPost: (scrollToComments: Boolean) -> Unit
) {
    val authorized = applicationViewModel.authState == AuthState.AUTHORIZED
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { /*TODO*/ }, enabled = authorized) {
                Icon(
                    Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(R.string.score_up)
                )
            }
            Text(post.score.total.toString())
            IconButton(onClick = { /*TODO*/ }, enabled = authorized) {
                Icon(
                    Icons.Filled.ArrowDownward,
                    contentDescription = stringResource(R.string.score_down)
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 24.dp)
            ) { openPost(true) }
        ) {
            Text(post.commentCount.toString())
            Icon(
                Icons.Outlined.Comment,
                contentDescription = stringResource(R.string.comments),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .offset(y = 2.dp)
            )
        }
        val isFavorited =
            applicationViewModel.isFavorited(post)
        IconButton(
            onClick = {
                applicationViewModel.handleFavoritePost(post)
            },
            enabled = authorized
        ) {
            Crossfade(targetState = isFavorited) {
                if (it) Icon(
                    Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.unfavorite)
                ) else Icon(
                    Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.favorite)
                )
            }
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                Icons.Default.Share,
                contentDescription = stringResource(R.string.share)
            )
        }
    }
}