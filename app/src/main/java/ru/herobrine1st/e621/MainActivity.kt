package ru.herobrine1st.e621

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.StatFs
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.room.Room
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.util.CoilUtils
import okhttp3.Cache
import okhttp3.OkHttpClient
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.api.LocalAPI
import ru.herobrine1st.e621.net.RateLimitInterceptor
import ru.herobrine1st.e621.ui.ActionBarMenu
import ru.herobrine1st.e621.ui.SnackbarController
import ru.herobrine1st.e621.ui.screen.Home
import ru.herobrine1st.e621.ui.screen.Screens
import ru.herobrine1st.e621.ui.screen.posts.Post
import ru.herobrine1st.e621.ui.screen.posts.PostsScreenNavigationComposable
import ru.herobrine1st.e621.ui.screen.search.Search
import ru.herobrine1st.e621.ui.screen.settings.Settings
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklist
import ru.herobrine1st.e621.ui.theme.E621Theme
import ru.herobrine1st.e621.util.FavouritesSearchOptions
import ru.herobrine1st.e621.util.PostsSearchOptions
import java.io.File


class MainActivity : ComponentActivity() {
    companion object {
        val TAG = MainActivity::class.simpleName
        const val DISK_CACHE_PERCENTAGE = 0.02
        const val MIN_DISK_CACHE_SIZE_BYTES = 10L * 1024 * 1024
        const val MAX_DISK_CACHE_SIZE_BYTES = 150L * 1024 * 1024
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val screen by remember { derivedStateOf { Screens.byRoute[navBackStackEntry?.destination?.route] } }
                val applicationViewModel: ApplicationViewModel =
                    viewModel(factory = ApplicationViewModel.Factory(db, api))
                val scaffoldState = rememberScaffoldState()

                LaunchedEffect(true) {
                    applicationViewModel.loadAllFromDatabase()
                }

                SnackbarController(applicationViewModel, scaffoldState)
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
                                ActionBarMenu(navController, applicationViewModel)
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
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        CompositionLocalProvider(LocalDatabase provides db, LocalAPI provides api) {
                            NavHost(
                                navController = navController,
                                startDestination = Screens.Home.route
                            ) {
                                composable(Screens.Home.route) {
                                    Home(navController, applicationViewModel)
                                }
                                composable(
                                    Screens.Search.route,
                                    Screens.Search.arguments
                                ) { entry ->
                                    val arguments: Bundle =
                                        entry.arguments!!

                                    val searchOptions = arguments.getParcelable("query")
                                        ?: PostsSearchOptions.DEFAULT
                                    Search(searchOptions) {
                                        navController.popBackStack()
                                        navController.navigate(
                                            Screens.Posts.buildRoute {
                                                addArgument("query", it)
                                            }
                                        )
                                    }
                                }
                                composable(Screens.Posts.route, Screens.Posts.arguments) {
                                    val searchOptions =
                                        it.arguments!!.getParcelable<PostsSearchOptions>("query")!!
                                    PostsScreenNavigationComposable(
                                        searchOptions = searchOptions,
                                        applicationViewModel = applicationViewModel,
                                        navController = navController
                                    )
                                }
                                composable(Screens.Favourites.route, Screens.Favourites.arguments) {
                                    val arguments =
                                        it.arguments!!
                                    val searchOptions =
                                        remember { FavouritesSearchOptions(arguments.getString("user")) }
                                    PostsScreenNavigationComposable(
                                        searchOptions = searchOptions,
                                        applicationViewModel = applicationViewModel,
                                        navController = navController
                                    )
                                }
                                composable(Screens.Post.route, Screens.Post.arguments) {
                                    val arguments =
                                        it.arguments!!
                                    Post(
                                        applicationViewModel,
                                        arguments.getInt("id"),
                                        arguments.getBoolean("scrollToComments")
                                    )
                                }
                                composable(Screens.Settings.route) {
                                    Settings(navController)
                                }
                                composable(Screens.SettingsBlacklist.route) {
                                    SettingsBlacklist(applicationViewModel) {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        }
                    }
                }
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