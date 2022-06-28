package ru.herobrine1st.e621.module

import android.content.Context
import android.os.StatFs
import androidx.compose.runtime.staticCompositionLocalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.IAPI
import ru.herobrine1st.e621.net.AuthorizationInterceptor
import ru.herobrine1st.e621.net.RateLimitInterceptor
import ru.herobrine1st.e621.net.UserAgentInterceptor
import ru.herobrine1st.e621.util.objectMapper
import java.io.File
import javax.inject.Qualifier

val LocalAPI = staticCompositionLocalOf<IAPI> { error("No API found") }

@Module
@InstallIn(ActivityRetainedComponent::class)
class APIModule {
    @Provides
    @ActivityRetainedScoped
    @APIHttpClient
    fun provideAPIHttpClient(
        @ApplicationContext context: Context,
        authorizationInterceptor: AuthorizationInterceptor
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "okhttp").apply { mkdirs() }
        val size = (StatFs(cacheDir.absolutePath).let {
            it.blockCountLong * it.blockSizeLong
        } * DISK_CACHE_PERCENTAGE).toLong()
            .coerceIn(
                MIN_DISK_CACHE_SIZE_BYTES,
                MAX_DISK_CACHE_SIZE_BYTES
            )
        return OkHttpClient.Builder()
            .addNetworkInterceptor(RateLimitInterceptor(1.5))
            .addNetworkInterceptor(UserAgentInterceptor())
            .addInterceptor(authorizationInterceptor)
            .cache(Cache(cacheDir, size))
            .build()
    }

    @Provides
    @ActivityRetainedScoped
    fun provideRetrofit(@APIHttpClient okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .client(okHttpClient)
            .build()


    @Provides
    @ActivityRetainedScoped
    fun provideIAPI(retrofit: Retrofit): IAPI = retrofit.create(IAPI::class.java)

    companion object {
        const val DISK_CACHE_PERCENTAGE = 0.02
        const val MIN_DISK_CACHE_SIZE_BYTES = 10L * 1024 * 1024
        const val MAX_DISK_CACHE_SIZE_BYTES = 150L * 1024 * 1024
    }
}


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class APIHttpClient
