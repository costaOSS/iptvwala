package com.iptvwala.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iptvwala.data.local.dao.*
import com.iptvwala.data.local.entity.*

@Database(
    entities = [
        SourceEntity::class,
        ChannelEntity::class,
        EpgProgramEntity::class,
        WatchHistoryEntity::class,
        VodPositionEntity::class,
        EpgUrlEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EpgDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun vodPositionDao(): VodPositionDao
    abstract fun epgUrlDao(): EpgUrlDao
}
