/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.data.authorization.AuthorizationRepositoryImpl

//    private suspend fun updateBlacklistFromAccount() {
//        if (database.blacklistDao().count() != 0) return
//        val tags = api.getBlacklistedTags()
//        database.withTransaction {
//            tags.forEach {
//                database.blacklistDao().insert(BlacklistEntry(it, true))
//            }
//        }
//    }
//    //region Up/down votes
//
//    suspend fun vote(post: Post, vote: Int) {
//        assert(vote in -1..1)
//        val currentVote = database.voteDao().getVote(post.id) ?: 0
//        if (vote == 0) {
//            val score = api.vote(post.id, currentVote, false)
//            if (score.ourScore != 0) { // API does not send user's vote with post
//                assert(api.vote(post.id, score.ourScore, false).ourScore == 0)
//            }
//        } else {
//            assert(api.vote(post.id, vote, true).ourScore == vote)
//        }
//        database.voteDao().insertOrUpdate(post.id, vote)
//    }
//
//    suspend fun getPostVote(post: Post): Int {
//        return database.voteDao().getVote(post.id) ?: 0
//    }

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class AuthorizationModule {
    @Binds
    @ActivityRetainedScoped
    abstract fun bindAuthorizationRepository(authorizationRepositoryImpl: AuthorizationRepositoryImpl): AuthorizationRepository
}