package com.iptvwala.data.remote.parser

import com.iptvwala.data.local.entity.ChannelEntity
import com.iptvwala.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XtreamParser @Inject constructor(
    private val httpClient: OkHttpClient
) {
    suspend fun fetchAndParse(
        host: String,
        username: String,
        password: String,
        sourceId: Long
    ): XtreamResult = withContext(Dispatchers.IO) {
        val authUrl = "$host/c Panel_API.php?username=$username&password=$password"
        
        val request = Request.Builder()
            .url(authUrl)
            .header("User-Agent", "IPTVWala/1.0")
            .build()
        
        val response = httpClient.newCall(request).execute()
        val content = response.body?.string() ?: throw Exception("Empty response")
        
        val json = JSONObject(content)
        
        val categories = mutableMapOf<String, String>()
        val categoriesArray = json.optJSONArray("categories") ?: json.optJSONObject("available_channels")?.optJSONArray("categories")
        
        categoriesArray?.let {
            for (i in 0 until it.length()) {
                val cat = it.getJSONObject(i)
                val categoryId = cat.getString("category_id")
                val categoryName = cat.optString("category_name", cat.optString("category"))
                categories[categoryId] = categoryName
            }
        }
        
        val channels = mutableListOf<ChannelEntity>()
        val streams = json.optJSONObject("streams") ?: json.optJSONArray("available_channels")?.let { arr ->
            val streamsObj = JSONObject()
            for (i in 0 until arr.length()) {
                val channel = arr.getJSONObject(i)
                val id = channel.getString("stream_id")
                streamsObj.put(id, channel)
            }
            streamsObj
        }
        
        streams?.let { streamObj ->
            val keys = streamObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val stream = streamObj.getJSONObject(key)
                
                val streamId = stream.optString("stream_id", key)
                val name = stream.optString("name", stream.optString("stream_display_name", "Channel $streamId"))
                val logo = stream.optString("stream_icon", stream.optString("logo", null))
                val categoryId = stream.optString("category_id", stream.optString("category", null))
                val categoryName = categories[categoryId] ?: stream.optString("category_name", null)
                
                val streamType = stream.optString("stream_type", "live")
                val category = when {
                    streamType.equals("movie", ignoreCase = true) -> "VOD"
                    streamType.equals("series", ignoreCase = true) -> "SERIES"
                    else -> "LIVE"
                }
                
                val streamUrl = when {
                    stream.has("stream_url") -> stream.getString("stream_url")
                    stream.has("url") -> stream.getString("url")
                    else -> "$host/$username/$password/$streamId"
                }
                
                channels.add(
                    ChannelEntity(
                        sourceId = sourceId,
                        name = name,
                        logo = logo,
                        streamUrl = streamUrl,
                        groupTitle = categoryName,
                        tvgId = streamId,
                        category = category
                    )
                )
            }
        }
        
        XtreamResult(
            channels = channels,
            serverInfo = ServerInfo(
                url = host,
                username = username,
                password = password,
                expirationDate = json.optString("exp_date"),
                maxConnections = json.optInt("max_connections", 0),
                activeConnections = json.optInt("active_cons", 0)
            )
        )
    }

    data class XtreamResult(
        val channels: List<ChannelEntity>,
        val serverInfo: ServerInfo
    )

    data class ServerInfo(
        val url: String,
        val username: String,
        val password: String,
        val expirationDate: String?,
        val maxConnections: Int,
        val activeConnections: Int
    )
}
