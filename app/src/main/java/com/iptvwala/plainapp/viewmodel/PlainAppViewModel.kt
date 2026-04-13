package com.iptvwala.plainapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.iptvwala.core.base.BaseViewModel
import com.iptvwala.core.base.UiEffect
import com.iptvwala.core.base.UiEvent
import com.iptvwala.core.base.UiState
import com.iptvwala.domain.model.*
import com.iptvwala.domain.repository.ChannelRepository
import com.iptvwala.domain.repository.SourceRepository
import com.iptvwala.server.service.ServerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlainAppUiState(
    val channels: List<Channel> = emptyList(),
    val sources: List<Source> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val clipboardHistory: List<String> = emptyList(),
    val deviceName: String = "",
    val deviceIp: String = "",
    val appVersion: String = "",
    val uptime: Long = 0,
    val serverRunning: Boolean = false,
    val serverPort: Int = 8080,
    val volume: Int = 0
) : UiState

sealed class PlainAppEvent : UiEvent {
    data class KeyPressed(val key: String) : PlainAppEvent()
    data class ClipboardPaste(val text: String) : PlainAppEvent()
    data object ClipboardClear : PlainAppEvent()
    data class SourceRefresh(val sourceId: Long) : PlainAppEvent()
    data class SourceDelete(val sourceId: Long) : PlainAppEvent()
    data class ChannelPlay(val channelId: Long) : PlainAppEvent()
    data class FilePlay(val path: String) : PlainAppEvent()
    data class FileDelete(val path: String) : PlainAppEvent()
    data class AppLaunch(val packageName: String) : PlainAppEvent()
    data class AppSearch(val query: String) : PlainAppEvent()
    data object NotificationsClear : PlainAppEvent()
    data class NotificationClick(val packageName: String) : PlainAppEvent()
    data object CopyIp : PlainAppEvent()
    data class VolumeChange(val level: Int) : PlainAppEvent()
    data object WakeScreen : PlainAppEvent()
}

sealed class PlainAppEffect : UiEffect {
    data class ShowToast(val message: String) : PlainAppEffect()
    data object NavigateBack : PlainAppEffect()
}

@HiltViewModel
class PlainAppViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val sourceRepository: SourceRepository,
    private val serverState: ServerState
) : BaseViewModel<PlainAppUiState, PlainAppEvent, PlainAppEffect>() {
    
    init {
        loadData()
    }
    
    override fun createInitialState() = PlainAppUiState()
    
    override fun handleEvent(event: PlainAppEvent) {
        when (event) {
            is PlainAppEvent.KeyPressed -> sendKeyEvent(event.key)
            is PlainAppEvent.ClipboardPaste -> onPasteFromClipboard(event.text)
            PlainAppEvent.ClipboardClear -> onClearClipboard()
            is PlainAppEvent.SourceRefresh -> refreshSource(event.sourceId)
            is PlainAppEvent.SourceDelete -> deleteSource(event.sourceId)
            is PlainAppEvent.ChannelPlay -> onPlayChannel(event.channelId)
            is PlainAppEvent.FilePlay -> onPlayFile(event.path)
            is PlainAppEvent.FileDelete -> onDeleteFile(event.path)
            is PlainAppEvent.AppLaunch -> onLaunchApp(event.packageName)
            is PlainAppEvent.AppSearch -> onSearchApps(event.query)
            PlainAppEvent.NotificationsClear -> onClearNotifications()
            is PlainAppEvent.NotificationClick -> onNotificationClick(event.packageName)
            PlainAppEvent.CopyIp -> onCopyIp()
            is PlainAppEvent.VolumeChange -> onVolumeChange(event.level)
            PlainAppEvent.WakeScreen -> onWakeScreen()
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            combine(
                channelRepository.getAllChannels(),
                sourceRepository.getAllSources()
            ) { channels, sources ->
                Pair(channels, sources)
            }.collect { (channels, sources) ->
                setState {
                    copy(
                        channels = channels,
                        sources = sources
                    )
                }
            }
        }
        
        viewModelScope.launch {
            serverState.playbackState.collect { playback ->
            }
        }
        
        viewModelScope.launch {
            serverState.isRunning.collect { running ->
                setState { copy(serverRunning = running) }
            }
        }
        
        viewModelScope.launch {
            setState {
                copy(
                    deviceName = serverState.deviceName,
                    deviceIp = serverState.deviceIp,
                    appVersion = serverState.appVersion,
                    uptime = serverState.uptime,
                    serverPort = serverState.port.value,
                    files = serverState.getFiles(),
                    installedApps = serverState.getInstalledApps(),
                    notifications = serverState.getNotifications(),
                    volume = serverState.getVolume()
                )
            }
        }
    }
    
    fun sendKeyEvent(key: String) {
        serverState.injectKeyEvent(key)
    }
    
    fun onPasteFromClipboard(text: String) {
        serverState.setClipboard(text)
        setEffect(PlainAppEffect.ShowToast("Pasted to clipboard"))
    }
    
    fun onClearClipboard() {
        setState { copy(clipboardHistory = emptyList()) }
    }
    
    fun refreshSource(sourceId: Long) {
        viewModelScope.launch {
            sourceRepository.refreshSource(sourceId)
            setEffect(PlainAppEffect.ShowToast("Source refreshed"))
        }
    }
    
    fun deleteSource(sourceId: Long) {
        viewModelScope.launch {
            sourceRepository.deleteSource(sourceId)
            setEffect(PlainAppEffect.ShowToast("Source deleted"))
        }
    }
    
    fun onPlayChannel(channelId: Long) {
        viewModelScope.launch {
            val channel = channelRepository.getChannelById(channelId)
            channel?.let {
                serverState.playChannel(it)
                setEffect(PlainAppEffect.ShowToast("Playing ${it.name}"))
            }
        }
    }
    
    fun onPlayFile(path: String) {
        serverState.playUrl("file://$path", path.substringAfterLast("/"))
    }
    
    fun onDeleteFile(path: String) {
        try {
            java.io.File(path).delete()
            setState { copy(files = serverState.getFiles()) }
            setEffect(PlainAppEffect.ShowToast("File deleted"))
        } catch (e: Exception) {
            setEffect(PlainAppEffect.ShowToast("Failed to delete file"))
        }
    }
    
    fun onLaunchApp(packageName: String) {
        serverState.launchApp(packageName)
    }
    
    fun onSearchApps(query: String) {
    }
    
    fun onClearNotifications() {
        serverState.clearNotifications()
        setState { copy(notifications = emptyList()) }
    }
    
    fun onNotificationClick(packageName: String) {
        serverState.launchApp(packageName)
    }
    
    fun onCopyIp() {
        serverState.setClipboard("http://${serverState.deviceIp}:${serverState.port.value}")
        setEffect(PlainAppEffect.ShowToast("IP copied to clipboard"))
    }
    
    fun onVolumeChange(level: Int) {
        serverState.setVolume(level)
        setState { copy(volume = level) }
    }
    
    fun onWakeScreen() {
        serverState.wakeScreen()
    }
}
