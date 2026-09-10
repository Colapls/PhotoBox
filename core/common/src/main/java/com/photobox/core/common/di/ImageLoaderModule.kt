package com.photobox.core.common.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache(
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            )
            .diskCache(
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "coil_disk_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            )
            .build()
}
