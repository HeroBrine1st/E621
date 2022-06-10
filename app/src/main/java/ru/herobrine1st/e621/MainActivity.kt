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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.api.LocalAPI
import ru.herobrine1st.e621.database.Database
import ru.herobrine1st.e621.database.LocalDatabase
import ru.herobrine1st.e621.enumeration.AuthState
import ru.herobrine1st.e621.preference.BLACKLIST_ENABLED
import ru.herobrine1st.e621.preference.dataStore
import ru.herobrine1st.e621.preference.getPreference
import ru.herobrine1st.e621.preference.setPreference
import ru.herobrine1st.e621.ui.ActionBarMenu
import ru.herobrine1st.e621.ui.dialog.BlacklistTogglesDialog
import ru.herobrine1st.e621.ui.screen.Home
import ru.herobrine1st.e621.ui.screen.Screen
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
import java.io.IOException
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var db: Database

    @Inject
    lateinit var api: Api

    @Inject
    lateinit var snackbarMessagesFlow: MutableSharedFlow<SnackbarMessage>

    @Inject
    lateinit var snackbarAdapter: SnackbarAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                applicationContext.dataStore.data.first()
            } catch (e: IOException) {
                Log.e(TAG, "Exception reading preferences", e)
            } catch (t: Throwable) {
                Log.w(TAG, "Exception reading preferences", t)
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

                // VMs
                val applicationViewModel = viewModel<ApplicationViewModel>()

                // State
                val scaffoldState = rememberScaffoldState()
                var showBlacklistDialog by remember { mutableStateOf(false) }

                LaunchedEffect(true) {
                    applicationViewModel.loadAllFromDatabase()
                }
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
                                    navBackStackEntry?.LocalOwnersProvider(saveableStateHolder = rememberSaveableStateHolder()) {
                                        screen?.appBarActions?.invoke(
                                            this,
                                            navController,
                                            applicationViewModel
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
                            navBackStackEntry?.LocalOwnersProvider(saveableStateHolder = rememberSaveableStateHolder()) {
                                screen?.floatingActionButton?.invoke(applicationViewModel)
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
                                        authState = applicationViewModel.authState,
                                        onLogin = { u, p, cb ->
                                            applicationViewModel.authenticate(u, p, cb)
                                        },
                                        onLogout = {
                                            applicationViewModel.logout()
                                        },
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
                                    val isBlacklistEnabled by LocalContext.current.getPreference(
                                        BLACKLIST_ENABLED,
                                        defaultValue = true
                                    )

                                    Posts(
                                        searchOptions,
                                        isFavourite = { post ->
                                            applicationViewModel.isFavorited(post)
                                        },
                                        isHiddenByBlacklist = { post ->
                                            !applicationViewModel.isFavorited(post)
                                                    && isBlacklistEnabled
                                                    && applicationViewModel
                                                .blacklistPostPredicate.test(post)
                                        },
                                        isAuthorized = applicationViewModel.authState == AuthState.AUTHORIZED,
                                        onAddToFavourites = { post ->
                                            applicationViewModel.handleFavoritePost(post)
                                        },
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
                                    val isBlacklistEnabled by LocalContext.current.getPreference(
                                        BLACKLIST_ENABLED,
                                        defaultValue = true
                                    )

                                    Posts(
                                        searchOptions,
                                        isFavourite = { post ->
                                            applicationViewModel.isFavorited(post)
                                        },
                                        isHiddenByBlacklist = { post ->
                                            !applicationViewModel.isFavorited(post)
                                                    && isBlacklistEnabled
                                                    && applicationViewModel
                                                .blacklistPostPredicate.test(post)
                                        },
                                        isAuthorized = applicationViewModel.authState == AuthState.AUTHORIZED,
                                        onAddToFavourites = { post ->
                                            applicationViewModel.handleFavoritePost(post)
                                        }
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
                                    SettingsBlacklist(applicationViewModel) {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }
                    }
                }

                if (showBlacklistDialog)
                    BlacklistTogglesDialog(
                        isBlacklistEnabled = context.getPreference(BLACKLIST_ENABLED, true).value,
                        toggleBlacklist = {
                            coroutineScope.launch {
                                context.setPreference(BLACKLIST_ENABLED, it)
                            }
                        }
                    ) {
                        showBlacklistDialog = false
                    }
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
    }
}