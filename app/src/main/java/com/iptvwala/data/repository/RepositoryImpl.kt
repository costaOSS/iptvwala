package com.iptvwala.data.repository

import com.iptvwala.data.local.dao.ChannelDao
import com.iptvwala.data.local.dao.SourceDao
import com.iptvwala.data.local.entity.ChannelEntity
import com.iptvwala.data.local.entity.SourceEntity
import com.iptvwala.data.remote.parser.M3uParser
import com.iptvwala.data.remote.parser.XtreamParser
import com.iptvwala.domain.model.Channel
import com.iptvwala.domain.model.ChannelCategory
import com.iptvwala.domain.model.Source
import com.iptvwala.domain.model.SourceType
import com.iptvwala.domain.repository.ChannelRepository
import com.iptvwala.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val channelDao: ChannelDao
) : ChannelRepository {
    
    override fun getAllChannels(): Flow<List<Channel>> {
        return channelDao.getAllChannels().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getChannelsBySource(sourceId: Long): Flow<List<Channel>> {
        return channelDao.getChannelsBySource(sourceId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getFavoriteChannels(): Flow<List<Channel>> {
        return channelDao.getFavoriteChannels().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getRecentlyWatched(): Flow<List<Channel>> {
        return channelDao.getRecentlyWatched().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun searchChannels(query: String): Flow<List<Channel>> {
        return channelDao.searchChannels(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getAllGroups(): Flow<List<String>> {
        return channelDao.getAllGroups()
    }
    
    override fun getChannelsByGroup(group: String): Flow<List<Channel>> {
        return channelDao.getChannelsByGroup(group).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getChannelById(id: Long): Channel? {
        return channelDao.getChannelById(id)?.toDomain()
    }
    
    override suspend fun getChannelByUrl(url: String): Channel? {
        return channelDao.getChannelByUrl(url)?.toDomain()
    }
    
    override suspend fun toggleFavorite(channelId: Long) {
        val channel = channelDao.getChannelById(channelId) ?: return
        channelDao.setFavorite(channelId, !channel.isFavorite)
    }
    
    override suspend fun setLastWatched(channelId: Long, timestamp: Long) {
        channelDao.setLastWatched(channelId, timestamp)
    }
    
    private fun ChannelEntity.toDomain() = Channel(
        id = id,
        sourceId = sourceId,
        name = name,
        logo = logo,
        streamUrl = streamUrl,
        groupTitle = groupTitle,
        tvgId = tvgId,
        tvgName = tvgName,
        isFavorite = isFavorite,
        lastWatched = lastWatched,
        category = ChannelCategory.valueOf(category),
        catchupSource = catchupSource,
        catchupDays = catchupDays,
        channelNumber = channelNumber
    )
}

@Singleton
class SourceRepositoryImpl @Inject constructor(
    private val sourceDao: SourceDao,
    private val channelDao: ChannelDao,
    private val m3uParser: M3uParser,
    private val xtreamParser: XtreamParser
) : SourceRepository {
    
    override fun getAllSources(): Flow<List<Source>> {
        return sourceDao.getAllSources().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getSourceById(id: Long): Source? {
        return sourceDao.getSourceById(id)?.toDomain()
    }
    
    override suspend fun addSource(source: Source): Long {
        val entity = source.toEntity()
        return sourceDao.insertSource(entity)
    }
    
    override suspend fun updateSource(source: Source) {
        sourceDao.updateSource(source.toEntity())
    }
    
    override suspend fun deleteSource(id: Long) {
        channelDao.deleteChannelsBySource(id)
        sourceDao.deleteSourceById(id)
    }
    
    override suspend fun refreshSource(sourceId: Long) {
        val source = sourceDao.getSourceById(sourceId) ?: return
        
        sourceDao.setRefreshing(sourceId, true)
        
        try {
            val channels = when (SourceType.valueOf(source.type)) {
                SourceType.M3U -> m3uParser.fetchAndParse(source.url, sourceId)
                SourceType.XTREAM -> {
                    val host = source.host ?: source.url
                    val user = source.username ?: ""
                    val pass = source.password ?: ""
                    xtreamParser.fetchAndParse(host, user, pass, sourceId).channels
                }
            }
            
            channelDao.deleteChannelsBySource(sourceId)
            channelDao.insertChannels(channels)
            
            sourceDao.setRefreshSuccess(sourceId, System.currentTimeMillis(), channels.size)
        } catch (e: Exception) {
            sourceDao.setRefreshError(sourceId, e.message ?: "Unknown error")
        }
    }
    
    override suspend fun refreshAllSources() {
        val sources = sourceDao.getAllSources()
        sources.collect { list ->
            list.forEach { source ->
                refreshSource(source.id)
            }
        }
    }
    
    private fun SourceEntity.toDomain() = Source(
        id = id,
        name = name,
        url = url,
        type = SourceType.valueOf(type),
        username = username,
        password = password,
        host = host,
        isEnabled = isEnabled,
        lastRefresh = lastRefresh,
        channelCount = channelCount,
        errorMessage = errorMessage,
        isRefreshing = isRefreshing,
        autoRefreshInterval = com.iptvwala.domain.model.RefreshInterval.entries.find { 
            it.hours == autoRefreshInterval 
        } ?: com.iptvwala.domain.model.RefreshInterval.HOURS_12
    )
    
    private fun Source.toEntity() = SourceEntity(
        id = id,
        name = name,
        url = url,
        type = type.name,
        username = username,
        password = password,
        host = host,
        isEnabled = isEnabled,
        lastRefresh = lastRefresh,
        channelCount = channelCount,
        errorMessage = errorMessage,
        autoRefreshInterval = autoRefreshInterval.hours
    )
}
