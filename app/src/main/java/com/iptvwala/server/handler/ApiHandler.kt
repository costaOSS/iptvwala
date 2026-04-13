package com.iptvwala.server.handler

import com.iptvwala.domain.model.*
import com.iptvwala.domain.repository.ChannelRepository
import com.iptvwala.domain.repository.EpgRepository
import com.iptvwala.domain.repository.SourceRepository
import com.iptvwala.server.service.ServerState
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiHandler @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val sourceRepository: SourceRepository,
    private val epgRepository: EpgRepository,
    private val serverState: ServerState
) {

    fun handleRequest(uri: String, method: String, params: Map<String, String>): NanoHTTPD.Response? {
        return when {
            uri == "/api/status" -> handleStatus(method)
            uri == "/api/channels" && method == "GET" -> handleGetChannels(params)
            uri.startsWith("/api/channels/search") -> handleSearchChannels(params)
            uri == "/api/play" && method == "POST" -> handlePlay(params)
            uri == "/api/play/url" && method == "POST" -> handlePlayUrl(params)
            uri == "/api/sources" && method == "GET" -> handleGetSources()
            uri == "/api/sources/add" && method == "POST" -> handleAddSource(params)
            uri.startsWith("/api/sources/") && method == "DELETE" -> handleDeleteSource(uri)
            uri.startsWith("/api/sources/") && method == "POST" -> handleRefreshSource(uri)
            uri == "/api/favorites" -> handleGetFavorites()
            uri == "/api/favorites/toggle" && method == "POST" -> handleToggleFavorite(params)
            uri.startsWith("/api/epg/") -> handleGetEpg(uri)
            uri == "/api/files" -> handleGetFiles()
            uri == "/api/clipboard" && method == "GET" -> handleGetClipboard()
            uri == "/api/clipboard" && method == "POST" -> handleSetClipboard(params)
            uri == "/api/device" -> handleGetDevice()
            uri == "/api/apps" -> handleGetApps()
            uri == "/api/apps/launch" && method == "POST" -> handleLaunchApp(params)
            uri == "/api/notifications" -> handleGetNotifications()
            uri == "/api/screen/wake" && method == "POST" -> handleWakeScreen()
            uri == "/api/volume" && method == "GET" -> handleGetVolume()
            uri == "/api/volume" && method == "POST" -> handleSetVolume(params)
            uri.startsWith("/api/remote/key") -> handleRemoteKey(params)
            uri == "/" -> handleIndex()
            uri.startsWith("/ws") -> handleWebSocket(uri, NanoHTTPD.IdealSocket())
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", """{"error": "Not found"}""")
        }
    }

    private fun handleStatus(method: String): NanoHTTPD.Response {
        if (method != "GET") return methodNotAllowed()
        val playback = serverState.playbackState.value
        val json = JSONObject().apply {
            put("playing", playback.isPlaying)
            put("channel", playback.channel?.let {
                JSONObject().put("id", it.id).put("name", it.name).put("logo", it.logo)
            })
            put("position", playback.position)
            put("duration", playback.duration)
        }
        return jsonResponse(json)
    }

    private fun handleGetChannels(params: Map<String, String>): NanoHTTPD.Response {
        val channels = runBlocking { channelRepository.getAllChannels().first() }
        val jsonArray = JSONArray()
        channels.forEach { channel ->
            jsonArray.put(JSONObject().apply {
                put("id", channel.id)
                put("name", channel.name)
                put("logo", channel.logo)
                put("group", channel.groupTitle)
                put("favorite", channel.isFavorite)
                put("lastWatched", channel.lastWatched)
            })
        }
        return jsonResponse(jsonArray)
    }

    private fun handleSearchChannels(params: Map<String, String>): NanoHTTPD.Response {
        val query = params["q"] ?: ""
        val channels = runBlocking { channelRepository.searchChannels(query).first() }
        val jsonArray = JSONArray()
        channels.forEach { channel ->
            jsonArray.put(JSONObject().apply {
                put("id", channel.id)
                put("name", channel.name)
                put("logo", channel.logo)
                put("group", channel.groupTitle)
            })
        }
        return jsonResponse(jsonArray)
    }

    private fun handlePlay(params: Map<String, String>): NanoHTTPD.Response {
        val id = params["id"] ?: return badRequest("Missing channel id")
        runBlocking {
            val channel = channelRepository.getChannelById(id.toLongOrNull() ?: return@runBlocking)
            channel?.let {
                serverState.playChannel(it)
            }
        }
        return jsonResponse(JSONObject().put("success", true))
    }

    private fun handlePlayUrl(params: Map<String, String>): NanoHTTPD.Response {
        val url = params["url"] ?: return badRequest("Missing URL")
        val name = params["name"] ?: "Unknown"
        serverState.playUrl(url, name)
        return jsonResponse(JSONObject().put("success", true))
    }

    private fun handleGetSources(): NanoHTTPD.Response {
        val sources = runBlocking { sourceRepository.getAllSources().first() }
        val jsonArray = JSONArray()
        sources.forEach { source ->
            jsonArray.put(JSONObject().apply {
                put("id", source.id)
                put("name", source.name)
                put("url", source.url)
                put("type", source.type.name)
                put("channelCount", source.channelCount)
                put("lastRefresh", source.lastRefresh)
                put("error", source.errorMessage)
            })
        }
        return jsonResponse(jsonArray)
    }

    private fun handleAddSource(params: Map<String, String>): NanoHTTPD.Response {
        val url = params["url"] ?: return badRequest("Missing URL")
        val name = params["name"] ?: "Source"
        val type = params["type"] ?: "M3U"
        val username = params["user"]
        val password = params["pass"]
        
        runBlocking {
            val source = Source(
                name = name,
                url = url,
                type = if (type == "XTREAM") SourceType.XTREAM else SourceType.M3U,
                username = username,
                password = password
            )
            sourceRepository.addSource(source)
        }
        return jsonResponse(JSONObject().put("success", true))
    }

    private fun handleDeleteSource(uri: String): NanoHTTPD.Response {
        val id = uri.substringAfter("/api/sources/").substringBefore("/")
        runBlocking { sourceRepository.deleteSource(id.toLongOrNull() ?: 0) }
        return jsonResponse(JSONObject().put("success", true))
    }

    private fun handleRefreshSource(uri: String): NanoHTTPD.Response {
        val id = uri.substringAfter("/api/sources/").substringBefore("/")
        runBlocking { sourceRepository.refreshSource(id.toLongOrNull() ?: 0) }
        return jsonResponse(JSONObject().put("success", true))
    }

    private fun handleGetFavorites(): NanoHTTPD.Response {
        val favorites = runBlocking { channelRepository.getFavoriteChannels().first() }
        val jsonArray = JSONArray()
        favorites.forEach { channel ->
            jsonArray.put(JSONObject().apply {
                put("id", channel.id)
                put("name", channel.name)
                put("logo", channel.logo)
            })
        }
        return jsonResponse(jsonArray)
    }

    private fun handleToggleFavorite(params: Map<String, String>): NanoHTTPD.Response {
        val id = params["id"] ?: return badRequest("Missing id")
        runBlocking { channelRepository.toggleFavorite(id.toLongOrNull() ?: 0) }
        return jsonResponse(JSONObject().put("success", true))
    }

    private fun handleGetEpg(uri: String): NanoHTTPD.Response {
        val channelId = uri.substringAfter("/api/epg/")
        val programs = runBlocking {
            val current = epgRepository.getCurrentProgram(channelId)
            val next = epgRepository.getNextPrograms(channelId)
            listOf(current, *next.toTypedArray()).filterNotNull()
        }
        val jsonArray = JSONArray()
        programs.forEach { program ->
            jsonArray.put(JSONObject().apply {
                put("title", program.title)
                put("description", program.description)
                put("startTime", program.startTime)
                put("endTime", program.endTime)
            })
        }
        return jsonResponse(jsonArray)
    }

    private fun handleGetFiles(): NanoHTTPD.Response {
        val files = serverState.getFiles()
        val jsonArray = JSONArray()
        files.forEach { file ->
            jsonArray.put(JSONObject().apply {
                put("name", file.name)
                put("path", file.path)
                put("size", file.size)
                put("dateModified", file.dateModified)
                put("isDirectory", file.isDirectory)
            })
        }
        return jsonResponse(jsonArray)
    }

    private fun handleGetClipboard(): NanoHTTPD.Response {
        return jsonResponse(JSONObject().put("content", serverState.getClipboard()))
    }

    private fun handleSetClipboard(params: Map<String, String>): NanoHTTPD.Response {
        val text = params["text"] ?: return badRequest("Missing text")
        serverState.setClipboard(text)
        serverState.broadcastClipboardChange(text)
        return jsonResponse(JSONObject().put("success", true))
    }

    private fun handleGetDevice(): NanoHTTPD.Response {
        return jsonResponse(JSONObject().apply {
            put("name", serverState.deviceName)
            put("ip", serverState.deviceIp)
            put("androidVersion", serverState.androidVersion)
            put("appVersion", serverState.appVersion)
            put("uptime", serverState.uptime)
            put("storageUsed", serverState.storageUsed)
            put("storageTotal", serverState.storageTotal)
        })
    }

    private fun handleGetApps(): NanoHTTPD.Response {
        val apps = serverState.getInstalledApps()
        val jsonArray = JSONArray()
        apps.forEach { app ->
            jsonArray.put(JSONObject().apply {
                put("name", app.name)
                put("package", app.packageName)
                put("icon", app.iconBase64)
            })
        }
        return jsonResponse(jsonArray)
    }

    private fun handleLaunchApp(params: Map<String, String>): NanoHTTPD.Response {
        val packageName = params["package"] ?: return badRequest("Missing package")
        serverState.launchApp(packageName)
        return jsonResponse(JSONObject().put("success", true))
    }

    private fun handleGetNotifications(): NanoHTTPD.Response {
        val notifications = serverState.getNotifications()
        val jsonArray = JSONArray()
        notifications.forEach { notif ->
            jsonArray.put(JSONObject().apply {
                put("id", notif.id)
                put("package", notif.packageName)
                put("appName", notif.appName)
                put("title", notif.title)
                put("text", notif.text)
                put("timestamp", notif.timestamp)
            })
        }
        return jsonResponse(jsonArray)
    }

    private fun handleWakeScreen(): NanoHTTPD.Response {
        serverState.wakeScreen()
        return jsonResponse(JSONObject().put("success", true))
    }

    private fun handleGetVolume(): NanoHTTPD.Response {
        return jsonResponse(JSONObject().put("level", serverState.getVolume()))
    }

    private fun handleSetVolume(params: Map<String, String>): NanoHTTPD.Response {
        val level = params["level"]?.toIntOrNull() ?: return badRequest("Missing level")
        serverState.setVolume(level)
        return jsonResponse(JSONObject().put("success", true))
    }

    private fun handleRemoteKey(params: Map<String, String>): NanoHTTPD.Response {
        val key = params["key"] ?: return badRequest("Missing key")
        serverState.injectKeyEvent(key)
        return jsonResponse(JSONObject().put("success", true))
    }

    private fun handleIndex(): NanoHTTPD.Response {
        return newFixedLengthResponse(
            Response.Status.OK,
            "text/html",
            "<html><body><h1>IPTVwala PlainApp</h1><p>Web UI not bundled</p></body></html>"
        )
    }

    private fun handleWebSocket(uri: String, handshake: NanoHTTPD.IdealSocket): NanoHTTPD.Response {
        return newFixedLengthResponse(Response.Status.SWITCHING_PROTOCOLS, "application/json", "")
    }

    private fun jsonResponse(json: JSONObject): NanoHTTPD.Response {
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
    }

    private fun jsonResponse(json: JSONArray): NanoHTTPD.Response {
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString())
    }

    private fun badRequest(message: String): NanoHTTPD.Response {
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", """{"error": "$message"}""")
    }

    private fun methodNotAllowed(): NanoHTTPD.Response {
        return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", """{"error": "Method not allowed"}""")
    }
}
