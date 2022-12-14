package ru.herobrine1st.e621.ui.screen.favourites

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.ui.screen.Screen
import ru.herobrine1st.e621.ui.theme.ActionBarIconColor
import ru.herobrine1st.e621.util.PostsSearchOptions

@Composable
fun FavouritesAppBarActions(
    navController: NavHostController
) {
    val preferences = LocalPreferences.current
    val username by remember { derivedStateOf { if (preferences.hasAuth()) preferences.auth.username else null } }
    IconButton(onClick = {
        navController.navigate(
            Screen.Search.buildRoute {
                // Other arguments have default values
                addArgument("query", PostsSearchOptions(favouritesOf = username))
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