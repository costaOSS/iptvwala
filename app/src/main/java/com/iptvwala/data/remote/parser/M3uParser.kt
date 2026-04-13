package com.iptvwala.data.remote.parser

import com.iptvwala.data.local.entity.ChannelEntity
import com.iptvwala.domain.model.ChannelCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3uParser @Inject constructor(
    private val httpClient: OkHttpClient
) {
    suspend fun parse(content: String, sourceId: Long): List<ChannelEntity> = withContext(Dispatchers.Default) {
        val channels = mutableListOf<ChannelEntity>()
        val lines = content.lines()
        
        var i = 0
        var channelNumber = 1
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            when {
                line.startsWith("#EXTM3U") -> {
                    i++
                }
                line.startsWith("#EXTINF:") -> {
                    val extInf = parseExtInf(line)
                    i++
                    
                    if (i < lines.size) {
                        val streamUrl = lines[i].trim()
                        if (streamUrl.isNotEmpty() && !streamUrl.startsWith("#")) {
                            val category = when {
                                streamUrl.contains(".mp4") || streamUrl.contains(".mkv") -> "VOD"
                                streamUrl.contains("/series/") -> "SERIES"
                                else -> "LIVE"
                            }
                            
                            channels.add(
                                ChannelEntity(
                                    sourceId = sourceId,
                                    name = extInf.name,
                                    logo = extInf.tvgLogo,
                                    streamUrl = streamUrl,
                                    groupTitle = extInf.groupTitle,
                                    tvgId = extInf.tvgId,
                                    tvgName = extInf.tvgName,
                                    category = category,
                                    catchupSource = extInf.catchupSource,
                                    catchupDays = extInf.catchupDays,
                                    channelNumber = channelNumber++
                                )
                            )
                        }
                    }
                }
                else -> i++
            }
        }
        
        channels
    }

    suspend fun fetchAndParse(url: String, sourceId: Long): List<ChannelEntity> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "IPTVWala/1.0")
            .build()
        
        val response = httpClient.newCall(request).execute()
        val content = response.body?.string() ?: throw Exception("Empty response")
        parse(content, sourceId)
    }

    private fun parseExtInf(line: String): ExtInfData {
        var attributes = line.removePrefix("#EXTINF:").trim()
        
        val commaIndex = attributes.indexOf(',')
        val name = if (commaIndex >= 0) {
            attributes.substring(commaIndex + 1).trim()
        } else {
            "Unknown Channel"
        }
        
        if (commaIndex >= 0) {
            attributes = attributes.substring(0, commaIndex)
        }
        
        val parts = attributes.split(Regex("\\s+(?=[^=]+=)"))
        val map = mutableMapOf<String, String>()
        
        for (part in parts) {
            val eqIndex = part.indexOf('=')
            if (eqIndex >= 0) {
                var key = part.substring(0, eqIndex).trim()
                var value = part.substring(eqIndex + 1).trim().removeSurrounding("\"", "\"")
                if (value.isEmpty() && key != "tvg-logo") {
                    continue
                }
                map[key] = value
            }
        }
        
        return ExtInfData(
            name = name,
            tvgId = map["tvg-id"],
            tvgName = map["tvg-name"],
            tvgLogo = map["tvg-logo"],
            groupTitle = map["group-title"],
            catchupSource = map["catchup-source"],
            catchupDays = map["catchup-days"]?.toIntOrNull() ?: 0
        )
    }

    private data class ExtInfData(
        val name: String,
        val tvgId: String? = null,
        val tvgName: String? = null,
        val tvgLogo: String? = null,
        val groupTitle: String? = null,
        val catchupSource: String? = null,
        val catchupDays: Int = 0
    )
}
