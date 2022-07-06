package ru.herobrine1st.e621

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.api.IAPI
import ru.herobrine1st.e621.database.Database
import ru.herobrine1st.e621.database.LocalDatabase
import ru.herobrine1st.e621.module.LocalAPI
import ru.herobrine1st.e621.preference.getPreferencesAsState
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.preference.updatePreferences
import ru.herobrine1st.e621.ui.ActionBarMenu
import ru.herobrine1st.e621.ui.dialog.BlacklistTogglesDialog
import ru.herobrine1st.e621.ui.screen.Screen
import ru.herobrine1st.e621.ui.screen.home.Home
import ru.herobrine1st.e621.ui.screen.posts.Post
import ru.herobrine1st.e621.ui.screen.posts.Posts
import ru.herobrine1st.e621.ui.screen.search.Search
import ru.herobrine1st.e621.ui.screen.settings.Settings
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklist
import ru.herobrine1st.e621.ui.snackbar.LocalSnackbar
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.ui.snackbar.SnackbarController
import ru.herobrine1st.e621.ui.snackbar.SnackbarMessage
import ru.herobrine1st.e621.ui.theme.E621Theme
import ru.herobrine1st.e621.util.FavouritesSearchOptions
import ru.herobrine1st.e621.util.PostsSearchOptions
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity @Inject constructor(
    private val db: Database,
    private val api: IAPI,
    private val snackbarMessagesFlow: MutableSharedFlow<SnackbarMessage>,
    private val snackbarAdapter: SnackbarAdapter
) : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                applicationContext.getPreferencesFlow().first()
            } catch (t: Throwable) {
                Log.e(TAG, "An error occurred while pre-reading preferences", t)
            }
        }

        setContent {
            E621Theme(window) {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                // Navigation
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val screen by remember { derivedStateOf { Screen.byRoute[navBackStackEntry?.destination?.route] } }

                // State
                val scaffoldState = rememberScaffoldState()
                val preferences by context.getPreferencesAsState()

                var showBlacklistDialog by remember { mutableStateOf(false) }
                SnackbarController(
                    snackbarMessagesFlow,
                    scaffoldState.snackbarHostState
                )
                CompositionLocalProvider(
                    LocalDatabase provides db,
                    LocalAPI provides api,
                    LocalSnackbar provides snackbarAdapter
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(stringResource(screen?.title ?: R.string.app_name))
                                },
                                backgroundColor = MaterialTheme.colors.primarySurface,
                                elevation = 12.dp,
                                actions = {
                                    val saveableStateHolder = rememberSaveableStateHolder()
                                    navBackStackEntry?.LocalOwnersProvider(saveableStateHolder = saveableStateHolder) {
                                        screen?.appBarActions?.invoke(
                                            this,
                                            navController
                                        )
                                    }
                                    ActionBarMenu(navController, onOpenBlacklistDialog = {
                                        showBlacklistDialog = true
                                    })
                                }
                            )
                        },
                        scaffoldState = scaffoldState,
                        floatingActionButton = {
                            val saveableStateHolder = rememberSaveableStateHolder()
                            navBackStackEntry?.LocalOwnersProvider(saveableStateHolder = saveableStateHolder) {
                                screen?.floatingActionButton?.invoke()
                            }
                        }
                    ) {
                        Surface(
                            color = MaterialTheme.colors.background
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Home.route
                            ) {
                                composable(Screen.Home.route) {
                                    Home(
                                        navigateToFavorites = {
                                            navController.navigate(Screen.Favourites.route)
                                        },
                                        navigateToSearch = {
                                            navController.navigate(Screen.Search.route)
                                        }
                                    )
                                }
                                composable(
                                    Screen.Search.route,
                                    Screen.Search.arguments
                                ) { entry ->
                                    val arguments: Bundle =
                                        entry.arguments!!

                                    val searchOptions = arguments.getParcelable("query")
                                        ?: PostsSearchOptions.DEFAULT
                                    Search(searchOptions) {
                                        navController.popBackStack()
                                        navController.navigate(
                                            Screen.Posts.buildRoute {
                                                addArgument("query", it)
                                            }
                                        )
                                    }
                                }
                                composable(Screen.Posts.route, Screen.Posts.arguments) {
                                    val searchOptions =
                                        it.arguments!!.getParcelable<PostsSearchOptions>("query")!!

                                    Posts(
                                        searchOptions,
                                        isBlacklistEnabled = preferences.blacklistEnabled,
                                    ) { post, scrollToComments ->
                                        navController.navigate(
                                            Screen.Post.buildRoute {
                                                addArgument("post", post)
                                                addArgument("scrollToComments", scrollToComments)
                                            }
                                        )
                                    }
                                }
                                composable(Screen.Favourites.route, Screen.Favourites.arguments) {
                                    val arguments =
                                        it.arguments!!
                                    val searchOptions =
                                        remember { FavouritesSearchOptions(arguments.getString("user")) }

                                    Posts(
                                        searchOptions,
                                        isBlacklistEnabled = preferences.blacklistEnabled
                                    ) { post, scrollToComments ->
                                        navController.navigate(
                                            Screen.Post.buildRoute {
                                                addArgument("post", post)
                                                addArgument("scrollToComments", scrollToComments)
                                            }
                                        )
                                    }
                                }
                                composable(Screen.Post.route, Screen.Post.arguments) {
                                    val arguments =
                                        it.arguments!!
                                    Post(
                                        arguments.getParcelable("post")!!,
                                        arguments.getBoolean("scrollToComments")
                                    )
                                }
                                composable(Screen.Settings.route) {
                                    Settings(navController)
                                }
                                composable(Screen.SettingsBlacklist.route) {
                                    SettingsBlacklist {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }
                    }
                }

                if (showBlacklistDialog)
                    BlacklistTogglesDialog(
                        isBlacklistEnabled = preferences.blacklistEnabled,
                        toggleBlacklist = { enabled: Boolean ->
                            coroutineScope.launch {
                                context.updatePreferences { setBlacklistEnabled(enabled) }
                            }
                        },
                        onClose = { showBlacklistDialog = false })
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
    }
}