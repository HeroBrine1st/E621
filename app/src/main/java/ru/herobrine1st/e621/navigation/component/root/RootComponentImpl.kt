/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.herobrine1st.e621.navigation.component.root

import android.content.Context
import android.os.StatFs
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.navigate
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate
import okhttp3.Cache
import okhttp3.OkHttpClient
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.module.InjectionCompanion
import ru.herobrine1st.e621.navigation.component.BlacklistTogglesDialogComponent
import ru.herobrine1st.e621.navigation.component.WikiComponent
import ru.herobrine1st.e621.navigation.component.home.HomeComponent
import ru.herobrine1st.e621.navigation.component.post.PostComponent
import ru.herobrine1st.e621.navigation.component.posts.PostListingComponent
import ru.herobrine1st.e621.navigation.component.root.RootComponent.Child
import ru.herobrine1st.e621.navigation.component.root.RootComponent.DialogChild
import ru.herobrine1st.e621.navigation.component.root.RootComponent.DialogConfig
import ru.herobrine1st.e621.navigation.component.search.SearchComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsAboutComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsAboutLibrariesComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsBlacklistComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsBlacklistEntryComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsLicenseComponent
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.config.Config.Home
import ru.herobrine1st.e621.navigation.config.Config.Post
import ru.herobrine1st.e621.navigation.config.Config.PostListing
import ru.herobrine1st.e621.navigation.config.Config.Search
import ru.herobrine1st.e621.navigation.config.Config.Settings
import ru.herobrine1st.e621.navigation.config.Config.Wiki
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.e621.util.FavouritesCache
import java.io.File

class RootComponentImpl(
    private val applicationContext: Context,
    private val injectionCompanion: InjectionCompanion,
    private val snackbarAdapterProvider: Lazy<SnackbarAdapter>,
    private val favouritesCacheProvider: Lazy<FavouritesCache>,
    private val exceptionReporterProvider: Lazy<ExceptionReporter>,
    private val blacklistRepositoryProvider: Lazy<BlacklistRepository>,
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext {

    private val instance = instanceKeeper.getOrCreate { Instance(applicationContext) }

    override val navigation = StackNavigation<Config>()
    override val stack = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Home,
        handleBackButton = true,
        childFactory = ::createChild
    )

    override val dialogNavigation = SlotNavigation<DialogConfig>()
    override val dialogSlot = childSlot(
        source = dialogNavigation,
        serializer = DialogConfig.serializer(),
        childFactory = ::createDialogChild
        // handleBackButton = false - dialogs handle it themselves
    )

    private fun createChild(
        configuration: Config,
        context: ComponentContext
    ): Child {
        return when (configuration) {
            is Home -> Child.Home(
                HomeComponent(
                    injectionCompanion.authorizationRepositoryLazy,
                    injectionCompanion.apiModule.apiLazy,
                    snackbarAdapterProvider.value,
                    blacklistRepositoryProvider.value,
                    navigation,
                    context
                )
            )
            is Search -> Child.Search(
                SearchComponent(
                    componentContext = context,
                    navigator = navigation,
                    initialSearchOptions = configuration.initialSearch,
                    api = injectionCompanion.apiModule.api,
                    applicationContext = applicationContext
                )
            )
            is PostListing -> Child.PostListing(
                PostListingComponent(
                    componentContext = context,
                    api = injectionCompanion.apiModule.api,
                    snackbar = snackbarAdapterProvider.value,
                    favouritesCache = favouritesCacheProvider.value,
                    exceptionReporter = exceptionReporterProvider.value,
                    searchOptions = configuration.search,
                    navigator = navigation,
                    applicationContext = applicationContext,
                    blacklistRepository = blacklistRepositoryProvider.value
                )
            )
            is Post -> Child.Post(
                PostComponent(
                    configuration.openComments,
                    configuration.query,
                    configuration.id,
                    configuration.post,
                    context,
                    navigation,
                    applicationContext,
                    exceptionReporterProvider.value,
                    injectionCompanion.apiModule.api,
                    favouritesCacheProvider.value,
                    snackbarAdapterProvider.value,
                    instance.mediaOkHttpClientProvider
                )
            )
            is Settings -> Child.Settings(SettingsComponent(context))
            is Settings.Blacklist -> Child.Settings.Blacklist(
                SettingsBlacklistComponent(
                    blacklistRepositoryProvider.value,
                    snackbarAdapterProvider.value,
                    navigation,
                    context
                )
            )
            is Settings.Blacklist.Entry ->
                Child.Settings.Blacklist.Entry(
                    SettingsBlacklistEntryComponent(
                        context,
                        configuration.id,
                        configuration.query,
                        configuration.enabled,
                        blacklistRepositoryProvider.value,
                        navigation
                    )
                )
            is Settings.About -> Child.Settings.About(SettingsAboutComponent(context))
            is Settings.License -> Child.Settings.License(SettingsLicenseComponent(context))
            is Settings.AboutLibraries -> Child.Settings.AboutLibraries(
                SettingsAboutLibrariesComponent(context)
            )
            is Wiki -> Child.Wiki(
                WikiComponent(
                    configuration.tag,
                    context,
                    injectionCompanion.apiModule.api,
                    snackbarAdapterProvider.value,
                    exceptionReporterProvider.value,
                    navigation
                )
            )
        }
    }

    private fun createDialogChild(
        configuration: DialogConfig,
        componentContext: ComponentContext
    ): DialogChild =
        @Suppress("USELESS_IS_CHECK")
        when (configuration) {
            is DialogConfig -> DialogChild.BlacklistToggles(
                BlacklistTogglesDialogComponent(
                    onClose = {
                        dialogNavigation.navigate { null }
                    },
                    blacklistRepositoryProvider.value,
                    applicationContext,
                    componentContext
                )
            )
        }

    // Used to save instances for DI into components
    private class Instance(val applicationContext: Context) : InstanceKeeper.Instance {
        val mediaOkHttpClientProvider = lazy {
            val cacheDir = File(applicationContext.cacheDir, "video").apply { mkdirs() }
            val size = (StatFs(cacheDir.absolutePath).let {
                it.blockCountLong * it.blockSizeLong
            } * DISK_CACHE_PERCENTAGE).toLong()
                .coerceIn(
                    MIN_DISK_CACHE_SIZE_BYTES,
                    MAX_DISK_CACHE_SIZE_BYTES
                )
            OkHttpClient.Builder()
                .cache(Cache(cacheDir, size))
                .build()
        }

        override fun onDestroy() {

        }

        companion object {
            const val DISK_CACHE_PERCENTAGE = 0.05
            const val MIN_DISK_CACHE_SIZE_BYTES = 10L * 1024 * 1024
            const val MAX_DISK_CACHE_SIZE_BYTES = 1024L * 1024 * 1024
        }
    }
}