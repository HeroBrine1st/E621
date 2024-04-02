/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import ru.herobrine1st.e621.navigation.component.BlacklistTogglesDialogComponent
import ru.herobrine1st.e621.navigation.component.PostMediaComponent
import ru.herobrine1st.e621.navigation.component.WikiComponent
import ru.herobrine1st.e621.navigation.component.home.HomeComponent
import ru.herobrine1st.e621.navigation.component.post.PostComponent
import ru.herobrine1st.e621.navigation.component.posts.PostListingComponent
import ru.herobrine1st.e621.navigation.component.search.SearchComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsAboutComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsAboutLibrariesComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsBlacklistComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsBlacklistEntryComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsComponent
import ru.herobrine1st.e621.navigation.component.settings.SettingsLicenseComponent
import ru.herobrine1st.e621.navigation.config.Config

interface RootComponent {
    sealed interface Child {
        class Home(val component: HomeComponent) : Child
        class Search(val component: SearchComponent) : Child
        class PostListing(val component: PostListingComponent) : Child
        class Post(val component: PostComponent) : Child
        class Wiki(val component: WikiComponent) : Child
        class PostMedia(val component: PostMediaComponent) : Child
        class Settings(val component: SettingsComponent) : Child {
            class Blacklist(val component: SettingsBlacklistComponent) : Child {
                class Entry(val component: SettingsBlacklistEntryComponent) : Child
            }

            class About(val component: SettingsAboutComponent) : Child
            class License(val component: SettingsLicenseComponent) : Child
            class AboutLibraries(val component: SettingsAboutLibrariesComponent) : Child
        }
    }

    // Global dialogs only !!
    // I mean, only dialogs that are not bound to component (hence, bind 'em to the root component)
    sealed interface DialogChild {
        class BlacklistToggles(val component: BlacklistTogglesDialogComponent) : DialogChild
    }

    @Serializable
    @Polymorphic
    sealed interface DialogConfig {
        @Serializable
        data object BlacklistToggles : DialogConfig
    }

    val navigation: StackNavigation<Config>
    val stack: Value<ChildStack<Config, Child>>

    val dialogNavigation: SlotNavigation<DialogConfig>
    val dialogSlot: Value<ChildSlot<DialogConfig, DialogChild>>
}

