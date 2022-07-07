package ru.herobrine1st.e621.ui.screen.favourites

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.screen.Screen
import ru.herobrine1st.e621.ui.screen.posts.logic.PostsViewModel
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor
import ru.herobrine1st.e621.util.PostsSearchOptions

@Composable
fun FavouritesAppBarActions(
    navController: NavHostController,
    viewModel: PostsViewModel = hiltViewModel() // Favourites screen uses this VM too
) {
    val ownUsername by viewModel.usernameFlow.collectAsState(initial = null) // Machine should be way faster than human
    IconButton(onClick = {
        navController.navigate(
            Screen.Search.buildRoute {
                // Other arguments have default values
                addArgument("query", PostsSearchOptions(favouritesOf = ownUsername))
            }
        )
    }) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(R.string.search),
            tint = ActionBarIconColor
        )
    }
}