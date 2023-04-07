/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.net.AuthorizationInterceptor
import ru.herobrine1st.e621.net.RateLimitInterceptor
import ru.herobrine1st.e621.net.UserAgentInterceptor
import ru.herobrine1st.e621.util.*
import java.io.File
import java.net.*
import javax.inject.Qualifier

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
            .addNetworkInterceptor(UserAgentInterceptor(USER_AGENT))
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
    fun provideAPI(retrofit: Retrofit): API = retrofit.create(API::class.java)

    companion object {
        const val DISK_CACHE_PERCENTAGE = 0.02
        const val MIN_DISK_CACHE_SIZE_BYTES = 10L * 1024 * 1024
        const val MAX_DISK_CACHE_SIZE_BYTES = 150L * 1024 * 1024
    }
}


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class APIHttpClient

