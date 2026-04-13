package com.iptvwala.data.remote.parser

import com.iptvwala.data.local.entity.EpgProgramEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

@Singleton
class XmltvParser @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val inputDateFormats = listOf(
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
    )

    private val outputDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun parse(content: String): List<EpgProgramEntity> = withContext(Dispatchers.Default) {
        val programs = mutableListOf<EpgProgramEntity>()
        
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(content.byteInputStream())
            
            val channelElements = document.getElementsByTagName("channel")
            val channelNames = mutableMapOf<String, String>()
            
            for (i in 0 until channelElements.length) {
                val channel = channelElements.item(i) as Element
                val channelId = channel.getAttribute("id")
                val displayName = channel.getElementsByTagName("display-name").item(0)?.textContent
                if (displayName != null) {
                    channelNames[channelId] = displayName
                }
            }
            
            val programmeElements = document.getElementsByTagName("programme")
            
            for (i in 0 until programmeElements.length) {
                val programme = programmeElements.item(i) as Element
                
                val channelId = programme.getAttribute("channel")
                    ?: programme.getAttribute("channel-id")
                    ?: continue
                
                val startStr = programme.getAttribute("start")
                val stopStr = programme.getAttribute("stop")
                
                val startTime = parseDate(startStr)
                val endTime = parseDate(stopStr)
                
                if (startTime == null || endTime == null) continue
                
                val title = programme.getElementsByTagName("title").item(0)?.textContent ?: continue
                val desc = programme.getElementsByTagName("desc").item(0)?.textContent
                val icon = programme.getElementsByTagName("icon").item(0)?.let { (it as? Element)?.getAttribute("src") }
                val category = programme.getElementsByTagName("category").item(0)?.textContent
                
                programs.add(
                    EpgProgramEntity(
                        channelId = channelId,
                        title = title,
                        description = desc,
                        startTime = startTime,
                        endTime = endTime,
                        icon = icon,
                        category = category
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        programs
    }

    suspend fun fetchAndParse(url: String): List<EpgProgramEntity> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "IPTVWala/1.0")
            .build()
        
        val response = httpClient.newCall(request).execute()
        val content = response.body?.string() ?: throw Exception("Empty response")
        parse(content)
    }

    private fun parseDate(dateStr: String): Long? {
        for (format in inputDateFormats) {
            try {
                return format.parse(dateStr.replace(" +", " +").replace(" -", " -"))?.time
            } catch (e: Exception) {
                continue
            }
        }
        
        try {
            val cleaned = dateStr.replace(Regex("[^0-9Z+-]"), "")
            return outputDateFormat.parse(cleaned)?.time
        } catch (e: Exception) {
            return null
        }
    }
}
