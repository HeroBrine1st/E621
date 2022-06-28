package ru.herobrine1st.e621.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableSharedFlow
import ru.herobrine1st.e621.ui.snackbar.SnackbarMessage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SnackbarModule {
    @Provides
    @Singleton
    fun provideSnackbarMessageFlow(): MutableSharedFlow<SnackbarMessage> = MutableSharedFlow()
}