package ru.herobrine1st.e621

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.StatFs
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
import androidx.room.Room
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.util.CoilUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.OkHttpClient
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.api.LocalAPI
import ru.herobrine1st.e621.database.Database
import ru.herobrine1st.e621.database.LocalDatabase
import ru.herobrine1st.e621.net.RateLimitInterceptor
import ru.herobrine1st.e621.preference.BLACKLIST_ENABLED
import ru.herobrine1st.e621.preference.dataStore
import ru.herobrine1st.e621.preference.getPreference
import ru.herobrine1st.e621.preference.setPreference
import ru.herobrine1st.e621.ui.ActionBarMenu
import ru.herobrine1st.e621.ui.SnackbarHost
import ru.herobrine1st.e621.ui.dialog.BlacklistTogglesDialog
import ru.herobrine1st.e621.ui.screen.Home
import ru.herobrine1st.e621.ui.screen.Screen
import ru.herobrine1st.e621.ui.screen.posts.Post
import ru.herobrine1st.e621.ui.screen.posts.Posts
import ru.herobrine1st.e621.ui.screen.search.Search
import ru.herobrine1st.e621.ui.screen.settings.Settings
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklist
import ru.herobrine1st.e621.ui.theme.E621Theme
import ru.herobrine1st.e621.util.FavouritesSearchOptions
import ru.herobrine1st.e621.util.PostsSearchOptions
import java.io.File
import java.io.IOException


class MainActivity : ComponentActivity() {
    companion object {
        val TAG = MainActivity::class.simpleName
        const val DISK_CACHE_PERCENTAGE = 0.02
        const val MIN_DISK_CACHE_SIZE_BYTES = 10L * 1024 * 1024
        const val MAX_DISK_CACHE_SIZE_BYTES = 150L * 1024 * 1024
    }

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

        val db: Database = Room.databaseBuilder(
            applicationContext,
            Database::class.java, BuildConfig.DATABASE_NAME
        ).build()

        Coil.setImageLoader {
            ImageLoader.Builder(applicationContext)
                .crossfade(true)
                .okHttpClient {
                    OkHttpClient.Builder()
                        .cache(CoilUtils.createDefaultCache(applicationContext))
                        .build()
                }
                .componentRegistry {
                    add(if (SDK_INT >= 28) ImageDecoderDecoder(applicationContext) else GifDecoder())
                }
                .build()
        }

        val api = createAPI()

        setContent {
            E621Theme(window) {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val screen by remember { derivedStateOf { Screen.byRoute[navBackStackEntry?.destination?.route] } }
                val applicationViewModel: ApplicationViewModel =
                    viewModel(factory = ApplicationViewModel.Factory(db, api))
                val scaffoldState = rememberScaffoldState()

                var showBlacklistDialog by remember { mutableStateOf(false) }

                LaunchedEffect(true) {
                    applicationViewModel.loadAllFromDatabase()
                }

                SnackbarHost(applicationViewModel, scaffoldState)
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
                        CompositionLocalProvider(LocalDatabase provides db, LocalAPI provides api) {
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
                                    Posts(
                                        searchOptions,
                                        applicationViewModel
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
                                        applicationViewModel
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
                                        applicationViewModel,
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
                        blacklistEntries = applicationViewModel.blacklistDoNotUseAsFilter,
                        isBlacklistEnabled = context.getPreference(BLACKLIST_ENABLED, true).value,
                        isBlacklistLoading = applicationViewModel.blacklistLoading,
                        isBlacklistUpdating = applicationViewModel.blacklistUpdating,
                        toggleBlacklist = {
                            coroutineScope.launch {
                                context.setPreference(BLACKLIST_ENABLED, it)
                            }
                        },
                        onApply = {
                            coroutineScope.launch {
                                applicationViewModel.applyBlacklistChanges()
                            }
                        },
                        onCancel = {
                                applicationViewModel.blacklistDoNotUseAsFilter.forEach { it.resetChanges() }
                        },
                        onClose = {
                            showBlacklistDialog = false
                        }
                    )
            }
        }
    }

    private fun createAPI(): Api {
        val cacheDir = File(applicationContext.cacheDir, "okhttp").apply { mkdirs() }
        val size = (StatFs(cacheDir.absolutePath).let {
            it.blockCountLong * it.blockSizeLong
        } * DISK_CACHE_PERCENTAGE).toLong()
            .coerceIn(MIN_DISK_CACHE_SIZE_BYTES, MAX_DISK_CACHE_SIZE_BYTES)
        val okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(RateLimitInterceptor(1.5))
            .cache(Cache(cacheDir, size))
            .build()
        return Api(okHttpClient)
    }
}