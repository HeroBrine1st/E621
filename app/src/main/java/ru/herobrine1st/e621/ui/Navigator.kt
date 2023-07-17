
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

package ru.herobrine1st.e621.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.router.slot.navigate
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.push
import ru.herobrine1st.e621.navigation.component.root.RootComponent
import ru.herobrine1st.e621.navigation.component.root.RootComponent.Child.Home
import ru.herobrine1st.e621.navigation.component.root.RootComponent.Child.Post
import ru.herobrine1st.e621.navigation.component.root.RootComponent.Child.PostListing
import ru.herobrine1st.e621.navigation.component.root.RootComponent.Child.Search
import ru.herobrine1st.e621.navigation.component.root.RootComponent.Child.Settings
import ru.herobrine1st.e621.navigation.component.root.RootComponent.Child.Wiki
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.preference.LocalPreferences
import ru.herobrine1st.e621.ui.animation.reducedSlide
import ru.herobrine1st.e621.ui.component.scaffold.rememberScreenSharedState
import ru.herobrine1st.e621.ui.screen.WikiScreen
import ru.herobrine1st.e621.ui.screen.home.Home
import ru.herobrine1st.e621.ui.screen.post.Post
import ru.herobrine1st.e621.ui.screen.posts.Posts
import ru.herobrine1st.e621.ui.screen.search.Search
import ru.herobrine1st.e621.ui.screen.settings.Settings
import ru.herobrine1st.e621.ui.screen.settings.SettingsAbout
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklist
import ru.herobrine1st.e621.ui.screen.settings.SettingsBlacklistEntry
import ru.herobrine1st.e621.ui.screen.settings.SettingsLicense
import ru.herobrine1st.e621.ui.screen.settings.SettingsLicenses

@Composable
fun Navigator(
    rootComponent: RootComponent,
    snackbarHostState: androidx.compose.material3.SnackbarHostState
) {
    val preferences = LocalPreferences.current
    val navigation = rootComponent.navigation

    val sharedState = rememberScreenSharedState(
        snackbarHostState = snackbarHostState,
        goToSettings = {
            navigation.navigate { stack ->
                if (stack.any { it is Config.Settings }) stack
                else stack + Config.Settings
            }
        },
        openBlacklistDialog = {
            rootComponent.dialogNavigation.navigate {
                // Usually it is not possible to click appbar while dialog is open
                // so it is safe to omit checks
                RootComponent.DialogConfig.BlacklistToggles
            }
        }
    )

    Children(
        stack = rootComponent.stack,
        modifier = Modifier.fillMaxSize(),
        animation = stackAnimation(
            fade() + reducedSlide(1 / 16f)
        )
    ) { child: Child.Created<*, RootComponent.Child> ->
        when (val instance: RootComponent.Child = child.instance) {
            is Home -> Home(
                screenSharedState = sharedState,
                component = instance.component
            )

            is Search -> Search(sharedState, instance.component)
            // There needs a centralized solution to cache preferences (particularly `hasAuth()`)
            // Although it is said not to cache it, looks like "LocalPreferences" is cache itself
            // So StateFlow should work too. Probably.
            // Then `preferences.hasAuth()` can be safely moved to components.
            is PostListing -> Posts(
                sharedState,
                instance.component,
                preferences.hasAuth()
            )

            is Post -> Post(
                screenSharedState = sharedState,
                component = instance.component,
                isAuthorized = preferences.hasAuth()
            )
            is Settings -> Settings(
                screenSharedState = sharedState,
                onNavigateToBlacklistSettings = {
                    navigation.push(Config.Settings.Blacklist)
                },
                onNavigateToAbout = {
                    navigation.push(Config.Settings.About)
                }
            )
            is Settings.Blacklist ->
                SettingsBlacklist(
                    screenSharedState = sharedState,
                    component = instance.component
                )
            is Settings.Blacklist.Entry -> SettingsBlacklistEntry(
                sharedState,
                instance.component
            )
            is Settings.About -> SettingsAbout(
                screenSharedState = sharedState,
                navigateToLicense = {
                    navigation.push(Config.Settings.License)
                },
                navigateToOssLicenses = {
                    navigation.push(Config.Settings.AboutLibraries)
                }
            )
            is Settings.License -> SettingsLicense(sharedState)
            is Settings.AboutLibraries -> SettingsLicenses(sharedState)
            is Wiki -> WikiScreen(sharedState, instance.component)
        }
    }
}