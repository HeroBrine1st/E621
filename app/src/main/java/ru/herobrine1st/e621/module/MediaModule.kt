package ru.herobrine1st.e621.module

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.android.exoplayer2.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val LocalExoPlayer = staticCompositionLocalOf<ExoPlayer> { error("No player found") }

@Module
@InstallIn(SingletonComponent::class)
class MediaModule {
    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context) = ExoPlayer.Builder(context).build()
}