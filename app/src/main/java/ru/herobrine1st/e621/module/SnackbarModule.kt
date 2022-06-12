package ru.herobrine1st.e621.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import ru.herobrine1st.e621.ui.snackbar.SnackbarMessage

@Module
@InstallIn(ActivityRetainedComponent::class)
class SnackbarModule {
    @Provides
    @ActivityRetainedScoped
    fun provideSnackbarMessageFlow(): MutableSharedFlow<SnackbarMessage> = MutableSharedFlow()
}