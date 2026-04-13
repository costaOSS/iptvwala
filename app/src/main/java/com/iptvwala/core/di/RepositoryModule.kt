package com.iptvwala.core.di

import com.iptvwala.data.repository.*
import com.iptvwala.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository = impl
    
    @Provides
    @Singleton
    fun provideSourceRepository(impl: SourceRepositoryImpl): SourceRepository = impl
    
    @Provides
    @Singleton
    fun provideEpgRepository(impl: EpgRepositoryImpl): EpgRepository = impl
    
    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl
    
    @Provides
    @Singleton
    fun provideWatchHistoryRepository(impl: WatchHistoryRepositoryImpl): WatchHistoryRepository = impl
}
