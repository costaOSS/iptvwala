package com.iptvwala.data.repository

import com.iptvwala.data.local.dao.WatchHistoryDao
import com.iptvwala.data.local.dao.VodPositionDao
import com.iptvwala.data.local.entity.WatchHistoryEntity
import com.iptvwala.data.local.entity.VodPositionEntity
import com.iptvwala.domain.model.WatchHistory
import com.iptvwala.domain.repository.WatchHistoryRepository
import com.iptvwala.domain.repository.VodPositionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryRepositoryImpl @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao
) : WatchHistoryRepository {
    
    override fun getRecentHistory(): Flow<List<WatchHistory>> {
        return watchHistoryDao.getRecentHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getLastWatched(channelId: Long): WatchHistory? {
        return watchHistoryDao.getLastWatched(channelId)?.toDomain()
    }
    
    override suspend fun addToHistory(channelId: Long, streamUrl: String, position: Long, duration: Long) {
        watchHistoryDao.insertHistory(
            WatchHistoryEntity(
                channelId = channelId,
                streamUrl = streamUrl,
                watchedAt = System.currentTimeMillis(),
                position = position,
                duration = duration
            )
        )
    }
    
    override suspend fun clearHistory() {
        watchHistoryDao.clearHistory()
    }
    
    override suspend fun getHistoryCount(): Int {
        return watchHistoryDao.getHistoryCount()
    }
    
    private fun WatchHistoryEntity.toDomain() = WatchHistory(
        id = id,
        channelId = channelId,
        streamUrl = streamUrl,
        watchedAt = watchedAt,
        position = position,
        duration = duration
    )
}

@Singleton
class VodPositionRepositoryImpl @Inject constructor(
    private val vodPositionDao: VodPositionDao
) : VodPositionRepository {
    
    override suspend fun getPosition(url: String): Long? {
        return vodPositionDao.getPosition(url)?.position
    }
    
    override suspend fun savePosition(url: String, position: Long, duration: Long) {
        vodPositionDao.savePosition(
            VodPositionEntity(
                streamUrl = url,
                position = position,
                duration = duration,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
    
    override suspend fun deletePosition(url: String) {
        vodPositionDao.deletePosition(url)
    }
}
