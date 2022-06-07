package ru.herobrine1st.e621.module

import android.content.Context
import android.os.StatFs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import okhttp3.Cache
import okhttp3.OkHttpClient
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.net.RateLimitInterceptor
import java.io.File

@Module
@InstallIn(ActivityRetainedComponent::class)
class APIModule {
    @Provides
    @ActivityRetainedScoped
    fun provideAPI(@ApplicationContext applicationContext: Context): Api {
        val cacheDir = File(applicationContext.cacheDir, "okhttp").apply { mkdirs() }
        val size = (StatFs(cacheDir.absolutePath).let {
            it.blockCountLong * it.blockSizeLong
        } * DISK_CACHE_PERCENTAGE).toLong()
            .coerceIn(
                MIN_DISK_CACHE_SIZE_BYTES,
                MAX_DISK_CACHE_SIZE_BYTES
            )
        val okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(RateLimitInterceptor(1.5))
            .cache(Cache(cacheDir, size))
            .build()
        return Api(okHttpClient)
    }

    companion object {
        const val DISK_CACHE_PERCENTAGE = 0.02
        const val MIN_DISK_CACHE_SIZE_BYTES = 10L * 1024 * 1024
        const val MAX_DISK_CACHE_SIZE_BYTES = 150L * 1024 * 1024
    }
}