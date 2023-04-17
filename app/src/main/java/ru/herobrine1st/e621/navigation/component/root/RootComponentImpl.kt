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
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.navigate
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.google.android.exoplayer2.ExoPlayer
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.navigation.component.BlacklistTogglesDialogComponent
import ru.herobrine1st.e621.navigation.component.home.HomeComponent
import ru.herobrine1st.e621.navigation.component.post.PostComponent
import ru.herobrine1st.e621.navigation.component.posts.PostListingComponent
import ru.herobrine1st.e621.navigation.component.root.RootComponent.*
import ru.herobrine1st.e621.navigation.component.search.SearchComponent
import ru.herobrine1st.e621.navigation.component.settings.*
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.config.Config.*
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.FavouritesCache
import ru.herobrine1st.e621.util.JacksonExceptionHandler

class RootComponentImpl(
    private val applicationContext: Context,
    private val authorizationRepositoryProvider: Lazy<AuthorizationRepository>,
    private val apiProvider: Lazy<API>,
    private val snackbarAdapterProvider: Lazy<SnackbarAdapter>,
    private val favouritesCacheProvider: Lazy<FavouritesCache>,
    private val jacksonExceptionHandlerProvider: Lazy<JacksonExceptionHandler>,
    private val blacklistRepositoryProvider: Lazy<BlacklistRepository>,
    private val exoPlayerProvider: Lazy<ExoPlayer>,
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext {

    override val navigation = StackNavigation<Config>()
    override val stack = childStack(
        source = navigation,
        initialConfiguration = Home,
        handleBackButton = true,
        childFactory = ::createChild
    )

    override val dialogNavigation = SlotNavigation<DialogConfig>()
    override val dialogSlot = childSlot(
        source = dialogNavigation,
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
                    authorizationRepositoryProvider,
                    apiProvider,
                    snackbarAdapterProvider.value,
                    navigation,
                    context
                )
            )
            is Search -> Child.Search(
                SearchComponent(
                    context,
                    navigation,
                    configuration.initialSearch
                )
            )
            is PostListing -> Child.PostListing(
                PostListingComponent(
                    componentContext = context,
                    api = apiProvider.value,
                    snackbar = snackbarAdapterProvider.value,
                    favouritesCache = favouritesCacheProvider.value,
                    jacksonExceptionHandler = jacksonExceptionHandlerProvider.value,
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
                    snackbarAdapterProvider.value,
                    jacksonExceptionHandlerProvider.value,
                    favouritesCacheProvider.value,
                    exoPlayerProvider.value,
                    apiProvider.value
                )
            )
            is Settings -> Child.Settings(SettingsComponent(context))
            is Settings.Blacklist -> Child.Settings.Blacklist(
                SettingsBlacklistComponent(
                    blacklistRepositoryProvider.value,
                    snackbarAdapterProvider.value,
                    context
                )
            )
            is Settings.About -> Child.Settings.About(SettingsAboutComponent(context))
            is Settings.License -> Child.Settings.License(SettingsLicenseComponent(context))
            is Settings.AboutLibraries -> Child.Settings.AboutLibraries(
                SettingsAboutLibrariesComponent(context)
            )
        }
    }

    private fun createDialogChild(configuration: DialogConfig, componentContext: ComponentContext): DialogChild =
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
}