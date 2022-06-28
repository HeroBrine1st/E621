package ru.herobrine1st.e621.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.data.authorization.AuthorizationRepositoryImpl

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class AuthorizationModule {
    @Binds
    @ActivityRetainedScoped
    abstract fun bindAuthorizationRepository(authorizationRepositoryImpl: AuthorizationRepositoryImpl): AuthorizationRepository
}