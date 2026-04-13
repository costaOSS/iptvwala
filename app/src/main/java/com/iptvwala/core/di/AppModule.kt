package com.iptvwala.core.di

import android.content.Context
import androidx.room.Room
import com.iptvwala.data.local.AppDatabase
import com.iptvwala.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "iptvwala.db"
        )
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .build()
    }
    
    @Provides
    fun provideSourceDao(database: AppDatabase): SourceDao = database.sourceDao()
    
    @Provides
    fun provideChannelDao(database: AppDatabase): ChannelDao = database.channelDao()
    
    @Provides
    fun provideEpgDao(database: AppDatabase): EpgDao = database.epgDao()
    
    @Provides
    fun provideWatchHistoryDao(database: AppDatabase): WatchHistoryDao = database.watchHistoryDao()
    
    @Provides
    fun provideVodPositionDao(database: AppDatabase): VodPositionDao = database.vodPositionDao()
    
    @Provides
    fun provideEpgUrlDao(database: AppDatabase): EpgUrlDao = database.epgUrlDao()
}
