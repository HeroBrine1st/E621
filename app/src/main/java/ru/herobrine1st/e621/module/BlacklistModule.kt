package ru.herobrine1st.e621.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.data.blacklist.BlacklistRepositoryImpl

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class BlacklistModule {
    @Binds
    abstract fun bindBlacklistRepository(
        blacklistRepositoryImpl: BlacklistRepositoryImpl
    ): BlacklistRepository
}