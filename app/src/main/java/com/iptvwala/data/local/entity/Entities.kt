package com.iptvwala.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val type: String,
    val username: String? = null,
    val password: String? = null,
    val host: String? = null,
    val isEnabled: Boolean = true,
    val lastRefresh: Long? = null,
    val channelCount: Int = 0,
    val errorMessage: String? = null,
    val autoRefreshInterval: Int = 12,
    val isRefreshing: Boolean = false
)

@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sourceId"),
        Index("groupTitle"),
        Index("isFavorite"),
        Index("name")
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceId: Long,
    val name: String,
    val logo: String? = null,
    val streamUrl: String,
    val groupTitle: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val isFavorite: Boolean = false,
    val lastWatched: Long? = null,
    val category: String = "LIVE",
    val catchupSource: String? = null,
    val catchupDays: Int = 0,
    val channelNumber: Int? = null
)

@Entity(
    tableName = "epg_programs",
    indices = [
        Index("channelId"),
        Index("startTime"),
        Index(value = ["channelId", "startTime"])
    ]
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long,
    val icon: String? = null,
    val category: String? = null
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelId: Long,
    val streamUrl: String,
    val watchedAt: Long,
    val position: Long = 0,
    val duration: Long = 0
)

@Entity(tableName = "vod_positions")
data class VodPositionEntity(
    @PrimaryKey
    val streamUrl: String,
    val position: Long,
    val duration: Long,
    val updatedAt: Long
)

@Entity(tableName = "epg_urls")
data class EpgUrlEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val name: String? = null,
    val lastRefresh: Long? = null
)
