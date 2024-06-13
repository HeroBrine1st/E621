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

package ru.herobrine1st.e621.navigation.component.posts

import android.util.Log
import androidx.annotation.IntRange
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.endpoint.posts.VoteEndpoint
import ru.herobrine1st.e621.api.model.PostId
import ru.herobrine1st.e621.util.ExceptionReporter


suspend fun handleVote(
    postId: PostId,
    @IntRange(from = -1, to = 1) vote: Int,
    api: API,
    exceptionReporter: ExceptionReporter,
): VoteEndpoint.Response? {
    return try {
        if (vote == 0) {
            // I'm a simple person: I see inefficient API - I write inefficient code
            val res = api.vote(postId, 1, false).getOrThrow()
            if (res.ourScore != 0)
                api.vote(postId, 1, false).getOrThrow()
            else res
        } else api.vote(postId, vote, true).getOrThrow()
    } catch (t: Throwable) {
        exceptionReporter.handleRequestException(t, showThrowable = true)
        null
    }?.also {
        if (it.ourScore != vote) {
            Log.w("VoteHandler", "API returned inconsistent response or workaround failed")
            Log.w("VoteHandler", "Expected ourScope=${vote}, got $it")
        }
    }
}