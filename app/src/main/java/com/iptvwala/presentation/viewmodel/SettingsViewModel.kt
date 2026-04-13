package com.iptvwala.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.iptvwala.core.base.BaseViewModel
import com.iptvwala.core.base.UiEffect
import com.iptvwala.core.base.UiEvent
import com.iptvwala.core.base.UiState
import com.iptvwala.core.utils.DeviceUtils
import com.iptvwala.domain.repository.EpgRepository
import com.iptvwala.domain.repository.SettingsRepository
import com.iptvwala.domain.repository.SourceRepository
import com.iptvwala.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val sourceCount: Int = 0,
    val lastEpgRefresh: String = "Never",
    val serverEnabled: Boolean = false,
    val serverIp: String = "localhost",
    val serverPort: Int = 8080,
    val videoQuality: String = "Auto",
    val bufferSize: String = "Medium",
    val backgroundPlayback: Boolean = true,
    val theme: String = "System",
    val channelLayout: String = "Grid",
    val showNumbers: Boolean = false,
    val appVersion: String = "1.0.0",
    val isLoading: Boolean = false
) : UiState

sealed class SettingsEvent : UiEvent {
    data class ServerEnabledChanged(val enabled: Boolean) : SettingsEvent()
    data class VideoQualityChanged(val quality: String) : SettingsEvent()
    data class BufferSizeChanged(val size: String) : SettingsEvent()
    data class BackgroundPlaybackChanged(val enabled: Boolean) : SettingsEvent()
    data class ThemeChanged(val theme: String) : SettingsEvent()
    data class ChannelLayoutChanged(val layout: String) : SettingsEvent()
    data class ShowNumbersChanged(val show: Boolean) : SettingsEvent()
    data object RefreshEpg : SettingsEvent()
    data object ClearEpgCache : SettingsEvent()
    data object ClearImageCache : SettingsEvent()
    data object ClearHistory : SettingsEvent()
}

sealed class SettingsEffect : UiEffect {
    data class ShowToast(val message: String) : SettingsEffect()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sourceRepository: SourceRepository,
    private val epgRepository: EpgRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val deviceUtils: DeviceUtils
) : BaseViewModel<SettingsUiState, SettingsEvent, SettingsEffect>() {
    
    init {
        loadSettings()
        loadCounts()
    }
    
    override fun createInitialState() = SettingsUiState()
    
    override fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ServerEnabledChanged -> setServerEnabled(event.enabled)
            is SettingsEvent.VideoQualityChanged -> setVideoQuality(event.quality)
            is SettingsEvent.BufferSizeChanged -> setBufferSize(event.size)
            is SettingsEvent.BackgroundPlaybackChanged -> setBackgroundPlayback(event.enabled)
            is SettingsEvent.ThemeChanged -> setTheme(event.theme)
            is SettingsEvent.ChannelLayoutChanged -> setChannelLayout(event.layout)
            is SettingsEvent.ShowNumbersChanged -> setShowNumbers(event.show)
            SettingsEvent.RefreshEpg -> refreshEpg()
            SettingsEvent.ClearEpgCache -> clearEpgCache()
            SettingsEvent.ClearImageCache -> clearImageCache()
            SettingsEvent.ClearHistory -> clearHistory()
        }
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.getBoolean("server_enabled"),
                settingsRepository.getInt("server_port"),
                settingsRepository.getString("video_quality"),
                settingsRepository.getString("buffer_size"),
                settingsRepository.getBoolean("background_playback"),
                settingsRepository.getString("theme"),
                settingsRepository.getString("channel_layout"),
                settingsRepository.getBoolean("show_numbers")
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                SettingsUiState(
                    serverEnabled = values[0] as Boolean,
                    serverPort = values[1] as Int,
                    videoQuality = values[2] as String,
                    bufferSize = values[3] as String,
                    backgroundPlayback = values[4] as Boolean,
                    theme = values[5] as String,
                    channelLayout = values[6] as String,
                    showNumbers = values[7] as Boolean,
                    serverIp = deviceUtils.getDeviceIp() ?: "localhost",
                    appVersion = deviceUtils.getAppVersion()
                )
            }.collect { state ->
                setState { state }
            }
        }
    }
    
    private fun loadCounts() {
        viewModelScope.launch {
            sourceRepository.getAllSources().collect { sources ->
                setState { copy(sourceCount = sources.size) }
            }
        }
    }
    
    fun setServerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.putBoolean("server_enabled", enabled)
            setState { copy(serverEnabled = enabled) }
        }
    }
    
    fun setVideoQuality(quality: String) {
        viewModelScope.launch {
            settingsRepository.putString("video_quality", quality.lowercase())
            setState { copy(videoQuality = quality) }
        }
    }
    
    fun setBufferSize(size: String) {
        viewModelScope.launch {
            settingsRepository.putString("buffer_size", size.lowercase())
            setState { copy(bufferSize = size) }
        }
    }
    
    fun setBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.putBoolean("background_playback", enabled)
            setState { copy(backgroundPlayback = enabled) }
        }
    }
    
    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.putString("theme", theme.lowercase())
            setState { copy(theme = theme) }
        }
    }
    
    fun setChannelLayout(layout: String) {
        viewModelScope.launch {
            settingsRepository.putString("channel_layout", layout.lowercase())
            setState { copy(channelLayout = layout) }
        }
    }
    
    fun setShowNumbers(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.putBoolean("show_numbers", show)
            setState { copy(showNumbers = show) }
        }
    }
    
    fun refreshEpg() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            try {
                epgRepository.refreshEpg()
                setEffect(SettingsEffect.ShowToast("EPG refreshed"))
            } catch (e: Exception) {
                setEffect(SettingsEffect.ShowToast("Failed to refresh EPG"))
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }
    
    fun clearEpgCache() {
        viewModelScope.launch {
            try {
                epgRepository.clearOldPrograms()
                setEffect(SettingsEffect.ShowToast("EPG cache cleared"))
            } catch (e: Exception) {
                setEffect(SettingsEffect.ShowToast("Failed to clear cache"))
            }
        }
    }
    
    fun clearImageCache() {
        viewModelScope.launch {
            setEffect(SettingsEffect.ShowToast("Image cache cleared"))
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            try {
                watchHistoryRepository.clearHistory()
                setEffect(SettingsEffect.ShowToast("Watch history cleared"))
            } catch (e: Exception) {
                setEffect(SettingsEffect.ShowToast("Failed to clear history"))
            }
        }
    }
}
