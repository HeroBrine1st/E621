package ru.herobrine1st.e621.ui.screen.favourites

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import ru.herobrine1st.e621.ApplicationViewModel
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.ui.screen.Screen
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor
import ru.herobrine1st.e621.util.PostsSearchOptions

@Composable
fun FavouritesAppBarActions(navController: NavHostController, applicationViewModel: ApplicationViewModel) {
    IconButton(onClick = {
        navController.navigate(
            Screen.Search.buildRoute {
                // Others have default values
                addArgument("query", PostsSearchOptions(favouritesOf = applicationViewModel.login))
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