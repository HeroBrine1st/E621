package ru.herobrine1st.e621.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.database.Database

@Module
@InstallIn(ActivityRetainedComponent::class)
class DatabaseModule {
    @Provides
    @ActivityRetainedScoped
    fun provideDatabase(@ApplicationContext applicationContext: Context): Database =
        Room.databaseBuilder(
            applicationContext,
            Database::class.java, BuildConfig.DATABASE_NAME
        ).build()
}