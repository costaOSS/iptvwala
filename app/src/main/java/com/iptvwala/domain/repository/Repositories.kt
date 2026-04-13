package com.iptvwala.domain.repository

import com.iptvwala.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getAllChannels(): Flow<List<Channel>>
    fun getChannelsBySource(sourceId: Long): Flow<List<Channel>>
    fun getFavoriteChannels(): Flow<List<Channel>>
    fun getRecentlyWatched(): Flow<List<Channel>>
    fun searchChannels(query: String): Flow<List<Channel>>
    fun getAllGroups(): Flow<List<String>>
    fun getChannelsByGroup(group: String): Flow<List<Channel>>
    suspend fun getChannelById(id: Long): Channel?
    suspend fun getChannelByUrl(url: String): Channel?
    suspend fun toggleFavorite(channelId: Long)
    suspend fun setLastWatched(channelId: Long, timestamp: Long)
}

interface SourceRepository {
    fun getAllSources(): Flow<List<Source>>
    suspend fun getSourceById(id: Long): Source?
    suspend fun addSource(source: Source): Long
    suspend fun updateSource(source: Source)
    suspend fun deleteSource(id: Long)
    suspend fun refreshSource(sourceId: Long)
    suspend fun refreshAllSources()
}

interface EpgRepository {
    suspend fun getCurrentProgram(channelId: String): EpgProgram?
    suspend fun getNextPrograms(channelId: String): List<EpgProgram>
    fun getProgramsForChannel(channelId: String, startTime: Long, endTime: Long): Flow<List<EpgProgram>>
    fun getAllProgramsInRange(startTime: Long, endTime: Long): Flow<List<EpgProgram>>
    suspend fun refreshEpg()
    suspend fun refreshEpgForSource(sourceUrl: String?)
    suspend fun clearOldPrograms()
    fun getAllEpgUrls(): Flow<List<String>>
    suspend fun addEpgUrl(url: String, name: String?)
    suspend fun deleteEpgUrl(id: Long)
}

interface WatchHistoryRepository {
    fun getRecentHistory(): Flow<List<WatchHistory>>
    suspend fun getLastWatched(channelId: Long): WatchHistory?
    suspend fun addToHistory(channelId: Long, streamUrl: String, position: Long, duration: Long)
    suspend fun clearHistory()
    suspend fun getHistoryCount(): Int
}

interface VodPositionRepository {
    suspend fun getPosition(url: String): Long?
    suspend fun savePosition(url: String, position: Long, duration: Long)
    suspend fun deletePosition(url: String)
}

interface SettingsRepository {
    fun getString(key: String, default: String = ""): Flow<String>
    fun getInt(key: String, default: Int = 0): Flow<Int>
    fun getBoolean(key: String, default: Boolean = false): Flow<Boolean>
    fun getLong(key: String, default: Long = 0L): Flow<Long>
    suspend fun putString(key: String, value: String)
    suspend fun putInt(key: String, value: Int)
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun putLong(key: String, value: Long)
    suspend fun clear(key: String)
    suspend fun clearAll()
}
