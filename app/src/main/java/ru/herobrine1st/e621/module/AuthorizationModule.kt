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