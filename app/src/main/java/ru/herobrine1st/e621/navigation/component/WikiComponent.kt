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

package ru.herobrine1st.e621.navigation.component

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.ApiException
import ru.herobrine1st.e621.api.MessageData
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.api.model.WikiPage
import ru.herobrine1st.e621.api.parseBBCode
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.ExceptionReporter

private const val TAG = "WikiComponent"

private const val WIKI_STATE_TAG = "WIKI_STATE_TAG"

class WikiComponent(
    val tag: Tag,
    componentContext: ComponentContext,
    private val api: API,
    private val snackbarAdapter: SnackbarAdapter,
    private val exceptionReporter: ExceptionReporter,
    private val stackNavigator: StackNavigator<Config>
) : ComponentContext by componentContext {
    private val lifecycleScope = LifecycleScope()

    var state by mutableStateOf<WikiState>(WikiState.Loading)

    init {
        stateKeeper.register(key = WIKI_STATE_TAG, strategy = WikiState.serializer()) {
            state
        }
        val restoredState = stateKeeper.consume(WIKI_STATE_TAG, strategy = WikiState.serializer())
        if (restoredState !is WikiState.Success) {
            fetchWikiPage()
        } else {
            lifecycleScope.launch { state = restoredState.parseWikiPage() }
        }
    }

    fun handleLinkClick(tag: Tag) {
        stackNavigator.pushIndexed {
            Config.Wiki(
                tag = tag,
                index = it
            )
        }
    }

    private fun fetchWikiPage() {
        state = WikiState.Loading
        lifecycleScope.launch {
            state = try {
                WikiState.Success(api.getWikiPage(tag).getOrThrow()).parseWikiPage()
            } catch (e: ApiException) {
                if(e.status == HttpStatusCode.NotFound) {
                    WikiState.NotFound
                } else {
                    Log.e(
                        TAG,
                        "Unknown API exception occurred while fetching wiki page for tag $tag",
                        e
                    )
                    snackbarAdapter.enqueueMessage(
                        R.string.unknown_api_error,
                        SnackbarDuration.Long
                    )
                    WikiState.Failure
                }
            } catch (t: Throwable) {
                exceptionReporter.handleRequestException(t, showThrowable = true)
                WikiState.Failure
            }
        }
    }

    // provided object is mutated!
    private suspend fun WikiState.Success.parseWikiPage(): WikiState.Success {
        // it may be true after configuration change
        if (isParsed) return this
        return withContext(Dispatchers.Default) {
            this@parseWikiPage.setParsed(
                parseBBCode(
                    this@parseWikiPage.result.body
                )
            )
            return@withContext this@parseWikiPage
        }
    }
}

@Serializable
@Polymorphic
sealed interface WikiState {

    @Serializable
    data object Loading : WikiState

    @Serializable
    class Success(val result: WikiPage) : WikiState {
        @Transient // it is a cache
        lateinit var parsed: List<MessageData>
            private set

        fun setParsed(v: List<MessageData>) {
            if (::parsed.isInitialized) throw RuntimeException("This property is write-once")
            parsed = v
        }

        val isParsed get() = ::parsed.isInitialized
    }

    @Serializable
    data object Failure : WikiState

    @Serializable
    data object NotFound : WikiState
}