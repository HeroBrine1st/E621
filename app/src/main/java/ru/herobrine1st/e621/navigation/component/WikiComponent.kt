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

package ru.herobrine1st.e621.navigation.component

import android.os.Parcelable
import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.statekeeper.consume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.*
import ru.herobrine1st.e621.api.model.WikiPage
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.ExceptionReporter
import java.io.IOException

private const val TAG = "WikiComponent"

private const val WIKI_STATE_TAG = "WIKI_STATE_TAG"

class WikiComponent(
    val tag: String,
    componentContext: ComponentContext,
    private val api: API,
    private val snackbarAdapter: SnackbarAdapter,
    private val exceptionReporter: ExceptionReporter
) : ComponentContext by componentContext {
    private val lifecycleScope = LifecycleScope()

    var state by mutableStateOf<WikiState>(WikiState.Loading)

    init {
        stateKeeper.register(WIKI_STATE_TAG) {
            state
        }
        val restoredState = stateKeeper.consume<WikiState>(WIKI_STATE_TAG)
        if (restoredState !is WikiState.Success) {
            fetchWikiPage()
        } else {
            lifecycleScope.launch { state = restoredState.parseWikiPage() }
        }
    }

    private fun fetchWikiPage() {
        state = WikiState.Loading
        lifecycleScope.launch {
            state = try {
                WikiState.Success(api.getWikiPage(tag)).parseWikiPage()
            } catch (e: NotFoundException) {
                WikiState.NotFound
            } catch (e: ApiException) {
                Log.e(
                    TAG,
                    "Unknown API exception occurred while fetching wiki page for tag $tag",
                    e
                )
                snackbarAdapter.enqueueMessage(R.string.unknown_api_error, SnackbarDuration.Long)
                WikiState.Failure
            } catch (e: IOException) {
                exceptionReporter.handleNetworkException(e)
                WikiState.Failure
            } catch (t: Throwable) {
                snackbarAdapter.enqueueMessage(
                    R.string.unknown_error,
                    SnackbarDuration.Indefinite
                )
                WikiState.Failure
            }
        }
    }

    // provided object is mutated!
    private suspend fun WikiState.Success.parseWikiPage(): WikiState.Success {
        return withContext(Dispatchers.Default) {
            this@parseWikiPage.setParsed(parseBBCode(this@parseWikiPage.result.body))
            return@withContext this@parseWikiPage
        }
    }
}

@Parcelize
sealed interface WikiState : Parcelable {

    @Parcelize
    object Loading : WikiState

    @Parcelize
    class Success(val result: WikiPage) : WikiState {
        @IgnoredOnParcel
        // MessageData is not parcelable and should not be

        lateinit var parsed: List<MessageData<*>>
            private set

        fun setParsed(v: List<MessageData<*>>) {
            if (::parsed.isInitialized) throw RuntimeException("This property is write-once")
            parsed = v
        }

    }

    @Parcelize
    object Failure : WikiState

    @Parcelize
    object NotFound : WikiState
}