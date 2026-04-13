package com.iptvwala.data.local.dao

import androidx.room.*
import com.iptvwala.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY name ASC")
    fun getAllSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getSourceById(id: Long): SourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceEntity): Long

    @Update
    suspend fun updateSource(source: SourceEntity)

    @Delete
    suspend fun deleteSource(source: SourceEntity)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun deleteSourceById(id: Long)

    @Query("UPDATE sources SET isRefreshing = :isRefreshing WHERE id = :id")
    suspend fun setRefreshing(id: Long, isRefreshing: Boolean)

    @Query("UPDATE sources SET lastRefresh = :timestamp, channelCount = :count, errorMessage = null WHERE id = :id")
    suspend fun setRefreshSuccess(id: Long, timestamp: Long, count: Int)

    @Query("UPDATE sources SET errorMessage = :error, isRefreshing = 0 WHERE id = :id")
    suspend fun setRefreshError(id: Long, error: String)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY groupTitle, name ASC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE sourceId = :sourceId ORDER BY name ASC")
    fun getChannelsBySource(sourceId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY lastWatched DESC LIMIT 50")
    fun getRecentlyWatched(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelById(id: Long): ChannelEntity?

    @Query("SELECT * FROM channels WHERE streamUrl = :url")
    suspend fun getChannelByUrl(url: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' OR groupTitle LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchChannels(query: String): Flow<List<ChannelEntity>>

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE groupTitle IS NOT NULL ORDER BY groupTitle ASC")
    fun getAllGroups(): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE groupTitle = :group ORDER BY name ASC")
    fun getChannelsByGroup(group: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE category = :category ORDER BY name ASC")
    fun getChannelsByCategory(category: String): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE channels SET lastWatched = :timestamp WHERE id = :id")
    suspend fun setLastWatched(id: Long, timestamp: Long)

    @Delete
    suspend fun deleteChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE sourceId = :sourceId")
    suspend fun deleteChannelsBySource(sourceId: Long)

    @Query("SELECT COUNT(*) FROM channels WHERE sourceId = :sourceId")
    suspend fun getChannelCount(sourceId: Long): Int
}

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND startTime <= :currentTime AND endTime > :currentTime ORDER BY startTime ASC LIMIT 1")
    suspend fun getCurrentProgram(channelId: String, currentTime: Long): EpgProgramEntity?

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND startTime > :currentTime ORDER BY startTime ASC LIMIT 2")
    suspend fun getNextPrograms(channelId: String, currentTime: Long): List<EpgProgramEntity>

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND startTime >= :startTime AND startTime < :endTime ORDER BY startTime ASC")
    fun getProgramsForChannel(channelId: String, startTime: Long, endTime: Long): Flow<List<EpgProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE startTime >= :startTime AND startTime < :endTime ORDER BY channelId, startTime ASC")
    fun getAllProgramsInRange(startTime: Long, endTime: Long): Flow<List<EpgProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: EpgProgramEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE endTime < :timestamp")
    suspend fun deleteOldPrograms(timestamp: Long)

    @Query("DELETE FROM epg_programs WHERE channelId = :channelId")
    suspend fun deleteProgramsForChannel(channelId: String)

    @Query("DELETE FROM epg_programs")
    suspend fun deleteAllPrograms()
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE channelId = :channelId ORDER BY watchedAt DESC LIMIT 1")
    suspend fun getLastWatched(channelId: Long): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistoryEntity): Long

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM watch_history")
    suspend fun getHistoryCount(): Int
}

@Dao
interface VodPositionDao {
    @Query("SELECT * FROM vod_positions WHERE streamUrl = :url")
    suspend fun getPosition(url: String): VodPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePosition(position: VodPositionEntity)

    @Query("DELETE FROM vod_positions WHERE streamUrl = :url")
    suspend fun deletePosition(url: String)
}

@Dao
interface EpgUrlDao {
    @Query("SELECT * FROM epg_urls ORDER BY name ASC")
    fun getAllUrls(): Flow<List<EpgUrlEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUrl(url: EpgUrlEntity): Long

    @Delete
    suspend fun deleteUrl(url: EpgUrlEntity)

    @Query("DELETE FROM epg_urls WHERE id = :id")
    suspend fun deleteUrlById(id: Long)
}
