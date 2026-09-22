package com.photobox.core.common.di

import com.photobox.core.common.AppDispatchers
import com.photobox.core.common.DefaultAppDispatchers
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DispatchersModule {
    @Binds
    @Singleton
    abstract fun bindAppDispatchers(impl: DefaultAppDispatchers): AppDispatchers
}