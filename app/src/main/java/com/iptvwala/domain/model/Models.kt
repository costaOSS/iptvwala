package com.iptvwala.domain.model

data class Channel(
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
    val category: ChannelCategory = ChannelCategory.LIVE,
    val catchupSource: String? = null,
    val catchupDays: Int = 0,
    val channelNumber: Int? = null
)

enum class ChannelCategory {
    LIVE, VOD, SERIES
}

data class Source(
    val id: Long = 0,
    val name: String,
    val url: String,
    val type: SourceType,
    val username: String? = null,
    val password: String? = null,
    val host: String? = null,
    val isEnabled: Boolean = true,
    val lastRefresh: Long? = null,
    val channelCount: Int = 0,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val autoRefreshInterval: RefreshInterval = RefreshInterval.HOURS_12
)

enum class SourceType {
    M3U, XTREAM
}

enum class RefreshInterval(val hours: Int) {
    HOURS_6(6), HOURS_12(12), HOURS_24(24), MANUAL(0)
}

data class EpgProgram(
    val id: Long = 0,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long,
    val icon: String? = null,
    val category: String? = null
)

data class WatchHistory(
    val id: Long = 0,
    val channelId: Long,
    val streamUrl: String,
    val watchedAt: Long,
    val position: Long = 0,
    val duration: Long = 0
)

data class ClipboardEntry(
    val id: Long = 0,
    val content: String,
    val timestamp: Long,
    val source: ClipboardSource = ClipboardSource.MANUAL
)

enum class ClipboardSource {
    MANUAL, BROWSER, INCOMING
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val channel: Channel? = null,
    val position: Long = 0,
    val duration: Long = 0,
    val isBuffering: Boolean = false,
    val error: String? = null,
    val playbackSpeed: Float = 1f
)

data class DeviceInfo(
    val name: String,
    val ip: String,
    val androidVersion: String,
    val appVersion: String,
    val uptime: Long,
    val storageUsed: Long,
    val storageTotal: Long
)

data class InstalledApp(
    val name: String,
    val packageName: String,
    val iconBase64: String? = null
)

data class NotificationItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val iconBase64: String? = null
)

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val dateModified: Long,
    val isDirectory: Boolean = false
)

data class ServerStatus(
    val isRunning: Boolean,
    val port: Int,
    val connectionCount: Int = 0,
    val uptime: Long = 0
)
