package com.iptvwala.server.service

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.iptvwala.R
import com.iptvwala.core.utils.DeviceUtils
import com.iptvwala.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlainAppServerService : Service() {
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): PlainAppServerService = this@PlainAppServerService
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_server_title))
            .setContentText(getString(R.string.notification_server_text, 8080))
            .setSmallIcon(R.drawable.ic_remote)
            .setOngoing(true)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
    
    companion object {
        const val CHANNEL_ID = "server_channel"
        const val NOTIFICATION_ID = 1002
    }
}

class ServerState @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val deviceUtils: DeviceUtils
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _port = MutableStateFlow(8080)
    val port: StateFlow<Int> = _port.asStateFlow()
    
    private var audioManager: AudioManager? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    val deviceName: String = deviceUtils.getDeviceName()
    val deviceIp: String = deviceUtils.getDeviceIp() ?: "127.0.0.1"
    val androidVersion: String = deviceUtils.getAndroidVersion()
    val appVersion: String = deviceUtils.getAppVersion()
    val uptime: Long get() = deviceUtils.getDeviceUptime()
    val storageUsed: Long get() {
        val (used, _) = deviceUtils.getStorageInfo()
        return used
    }
    val storageTotal: Long get() {
        val (_, total) = deviceUtils.getStorageInfo()
        return total
    }
    
    private val clipboardHistory = mutableListOf<ClipboardEntry>()
    private val webSocketClients = mutableListOf<WebSocketClient>()
    
    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    fun start(port: Int) {
        _port.value = port
        _isRunning.value = true
    }
    
    fun stop() {
        _isRunning.value = false
    }
    
    fun playChannel(channel: Channel) {
        _playbackState.value = PlaybackState(
            isPlaying = true,
            channel = channel
        )
        broadcastPlaybackChange()
    }
    
    fun playUrl(url: String, name: String) {
        _playbackState.value = PlaybackState(
            isPlaying = true,
            channel = Channel(0, 0, name, streamUrl = url)
        )
        broadcastPlaybackChange()
    }
    
    fun updatePlayback(position: Long, duration: Long, isPlaying: Boolean) {
        _playbackState.value = _playbackState.value.copy(
            position = position,
            duration = duration,
            isPlaying = isPlaying
        )
    }
    
    fun getFiles(): List<FileItem> {
        val downloadsDir = context.getExternalFilesDir(null) ?: return emptyList()
        return downloadsDir.listFiles()?.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                dateModified = file.lastModified(),
                isDirectory = file.isDirectory
            )
        } ?: emptyList()
    }
    
    fun getClipboard(): String {
        return clipboardHistory.lastOrNull()?.content ?: ""
    }
    
    fun setClipboard(text: String) {
        clipboardHistory.add(
            ClipboardEntry(
                content = text,
                timestamp = System.currentTimeMillis(),
                source = ClipboardSource.MANUAL
            )
        )
        if (clipboardHistory.size > 20) {
            clipboardHistory.removeAt(0)
        }
    }
    
    fun broadcastClipboardChange(text: String) {
        clipboardHistory.add(
            ClipboardEntry(
                content = text,
                timestamp = System.currentTimeMillis(),
                source = ClipboardSource.BROWSER
            )
        )
        webSocketClients.forEach { client ->
            client.send("clipboard:$text")
        }
    }
    
    fun getInstalledApps(): List<InstalledApp> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                pm.getLaunchIntentForPackage(app.packageName) != null &&
                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            }
            .mapNotNull { app ->
                try {
                    val icon = pm.getApplicationIcon(app.packageName)
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        icon.intrinsicWidth,
                        icon.intrinsicHeight,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    icon.setBounds(0, 0, canvas.width, canvas.height)
                    icon.draw(canvas)
                    val baos = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 50, baos)
                    
                    InstalledApp(
                        name = pm.getApplicationLabel(app).toString(),
                        packageName = app.packageName,
                        iconBase64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.name }
    }
    
    fun launchApp(packageName: String) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    private val notifications = mutableListOf<NotificationItem>()
    
    fun addNotification(notification: NotificationItem) {
        notifications.add(0, notification)
        if (notifications.size > 50) {
            notifications.removeAt(notifications.size - 1)
        }
    }
    
    fun getNotifications(): List<NotificationItem> = notifications.toList()
    
    fun clearNotifications() {
        notifications.clear()
    }
    
    fun wakeScreen() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            
            @Suppress("DEPRECATION")
            powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "IPTVwala::WakeScreen"
            ).acquire(5000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getVolume(): Int {
        return audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    }
    
    fun setVolume(level: Int) {
        audioManager?.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            level.coerceIn(0, 15),
            0
        )
    }
    
    fun injectKeyEvent(key: String) {
        try {
            val keyCode = when (key.uppercase()) {
                "UP" -> android.view.KeyEvent.KEYCODE_DPAD_UP
                "DOWN" -> android.view.KeyEvent.KEYCODE_DPAD_DOWN
                "LEFT" -> android.view.KeyEvent.KEYCODE_DPAD_LEFT
                "RIGHT" -> android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                "OK", "ENTER" -> android.view.KeyEvent.KEYCODE_DPAD_CENTER
                "BACK" -> android.view.KeyEvent.KEYCODE_BACK
                "HOME" -> android.view.KeyEvent.KEYCODE_HOME
                "MENU" -> android.view.KeyEvent.KEYCODE_MENU
                "PLAY" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY
                "PAUSE" -> android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
                "PLAY_PAUSE" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                "STOP" -> android.view.KeyEvent.KEYCODE_MEDIA_STOP
                "NEXT" -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                "PREVIOUS" -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
                else -> null
            }
            
            keyCode?.let {
                val injector = KeyEventInjector(context)
                injector.injectKeyEvent(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun broadcastPlaybackChange() {
        webSocketClients.forEach { client ->
            val state = _playbackState.value
            client.send("playback:${state.channel?.name}:${state.isPlaying}:${state.position}")
        }
    }
    
    fun addWebSocketClient(client: WebSocketClient) {
        webSocketClients.add(client)
    }
    
    fun removeWebSocketClient(client: WebSocketClient) {
        webSocketClients.remove(client)
    }
}

class WebSocketClient {
    private var socket: fi.iki.elonen.websocket.WebSocket? = null
    
    fun setSocket(socket: fi.iki.elonen.websocket.WebSocket) {
        this.socket = socket
    }
    
    fun send(message: String) {
        try {
            socket?.send(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class KeyEventInjector(private val context: Context) {
    
    fun injectKeyEvent(keyCode: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val uiAutomation = context.getSystemService(Context.UI_AUTOMATION_SERVICE) as? android.app.UiAutomation
                uiAutomation?.let {
                    val event = android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        keyCode
                    )
                    it.injectInputEvent(event, true)
                    
                    val upEvent = android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_UP,
                        keyCode
                    )
                    it.injectInputEvent(upEvent, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private class Intent
