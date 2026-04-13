package com.iptvwala.data.repository

import com.iptvwala.data.local.dao.EpgDao
import com.iptvwala.data.local.dao.EpgUrlDao
import com.iptvwala.data.local.dao.SourceDao
import com.iptvwala.data.local.entity.EpgProgramEntity
import com.iptvwala.data.local.entity.EpgUrlEntity
import com.iptvwala.data.remote.parser.XmltvParser
import com.iptvwala.domain.model.EpgProgram
import com.iptvwala.domain.repository.EpgRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepositoryImpl @Inject constructor(
    private val epgDao: EpgDao,
    private val epgUrlDao: EpgUrlDao,
    private val sourceDao: SourceDao,
    private val xmltvParser: XmltvParser
) : EpgRepository {
    
    override suspend fun getCurrentProgram(channelId: String): EpgProgram? {
        return epgDao.getCurrentProgram(channelId, System.currentTimeMillis())?.toDomain()
    }
    
    override suspend fun getNextPrograms(channelId: String): List<EpgProgram> {
        return epgDao.getNextPrograms(channelId, System.currentTimeMillis()).map { it.toDomain() }
    }
    
    override fun getProgramsForChannel(channelId: String, startTime: Long, endTime: Long): Flow<List<EpgProgram>> {
        return epgDao.getProgramsForChannel(channelId, startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getAllProgramsInRange(startTime: Long, endTime: Long): Flow<List<EpgProgram>> {
        return epgDao.getAllProgramsInRange(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun refreshEpg() {
        withContext(Dispatchers.IO) {
            val urls = epgUrlDao.getAllUrls().first()
            urls.forEach { urlEntity ->
                try {
                    val programs = xmltvParser.fetchAndParse(urlEntity.url)
                    epgDao.insertPrograms(programs)
                    epgUrlDao.insertUrl(urlEntity.copy(lastRefresh = System.currentTimeMillis()))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    override suspend fun refreshEpgForSource(sourceUrl: String?) {
        if (sourceUrl != null) {
            try {
                val programs = xmltvParser.fetchAndParse(sourceUrl)
                epgDao.insertPrograms(programs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        refreshEpg()
    }
    
    override suspend fun clearOldPrograms() {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        epgDao.deleteOldPrograms(sevenDaysAgo)
    }
    
    override fun getAllEpgUrls(): Flow<List<String>> {
        return epgUrlDao.getAllUrls().map { entities ->
            entities.map { it.url }
        }
    }
    
    override suspend fun addEpgUrl(url: String, name: String?) {
        epgUrlDao.insertUrl(EpgUrlEntity(url = url, name = name))
    }
    
    override suspend fun deleteEpgUrl(id: Long) {
        epgUrlDao.deleteUrlById(id)
    }
    
    private fun EpgProgramEntity.toDomain() = EpgProgram(
        id = id,
        channelId = channelId,
        title = title,
        description = description,
        startTime = startTime,
        endTime = endTime,
        icon = icon,
        category = category
    )
}
