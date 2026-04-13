package com.iptvwala.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.iptvwala.core.base.BaseViewModel
import com.iptvwala.core.base.UiEffect
import com.iptvwala.core.base.UiEvent
import com.iptvwala.core.base.UiState
import com.iptvwala.domain.model.Channel
import com.iptvwala.domain.model.EpgProgram
import com.iptvwala.domain.model.PlaybackState
import com.iptvwala.domain.repository.ChannelRepository
import com.iptvwala.domain.repository.EpgRepository
import com.iptvwala.domain.repository.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val currentChannel: Channel? = null,
    val currentProgram: EpgProgram? = null,
    val nextProgram: EpgProgram? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val playbackSpeed: Float = 1f,
    val error: String? = null,
    val channels: List<Channel> = emptyList(),
    val currentIndex: Int = -1
) : UiState

sealed class PlayerEvent : UiEvent {
    data class PlayChannel(val channelId: Long) : PlayerEvent()
    data object TogglePlayPause : PlayerEvent()
    data class SeekTo(val position: Long) : PlayerEvent()
    data class SetSpeed(val speed: Float) : PlayerEvent()
    data object NextChannel : PlayerEvent()
    data object PreviousChannel : PlayerEvent()
    data object Stop : PlayerEvent()
}

sealed class PlayerEffect : UiEffect {
    data class ShowError(val message: String) : PlayerEffect()
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository,
    private val watchHistoryRepository: WatchHistoryRepository
) : BaseViewModel<PlayerUiState, PlayerEvent, PlayerEffect>() {
    
    private var positionUpdateJob: Job? = null
    
    init {
        loadChannels()
    }
    
    override fun createInitialState() = PlayerUiState()
    
    override fun handleEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.PlayChannel -> playChannel(event.channelId)
            PlayerEvent.TogglePlayPause -> togglePlayPause()
            is PlayerEvent.SeekTo -> seekTo(event.position)
            is PlayerEvent.SetSpeed -> setPlaybackSpeed(event.speed)
            PlayerEvent.NextChannel -> playNextChannel()
            PlayerEvent.PreviousChannel -> playPreviousChannel()
            PlayerEvent.Stop -> stop()
        }
    }
    
    private fun loadChannels() {
        viewModelScope.launch {
            channelRepository.getAllChannels().collect { channels ->
                setState { copy(channels = channels) }
            }
        }
    }
    
    fun playChannel(channelId: Long) {
        viewModelScope.launch {
            val channel = channelRepository.getChannelById(channelId) ?: return@launch
            
            val index = currentState.channels.indexOfFirst { it.id == channelId }
            setState { 
                copy(
                    currentChannel = channel,
                    currentIndex = index,
                    isBuffering = true,
                    error = null
                ) 
            }
            
            loadEpgData(channel)
            
            channelRepository.setLastWatched(channelId, System.currentTimeMillis())
            
            watchHistoryRepository.addToHistory(
                channelId = channelId,
                streamUrl = channel.streamUrl,
                position = 0,
                duration = 0
            )
            
            startPositionUpdates()
        }
    }
    
    private fun loadEpgData(channel: Channel) {
        val channelId = channel.tvgId ?: channel.id.toString()
        
        viewModelScope.launch {
            val current = epgRepository.getCurrentProgram(channelId)
            val next = epgRepository.getNextPrograms(channelId)
            
            setState {
                copy(
                    currentProgram = current,
                    nextProgram = next.firstOrNull()
                )
            }
        }
    }
    
    fun togglePlayPause() {
        setState { copy(isPlaying = !isPlaying) }
    }
    
    fun seekTo(position: Long) {
        setState { copy(position = position) }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        setState { copy(playbackSpeed = speed) }
    }
    
    fun playNextChannel() {
        val channels = currentState.channels
        val currentIndex = currentState.currentIndex
        
        if (currentIndex < channels.size - 1) {
            val nextChannel = channels[currentIndex + 1]
            playChannel(nextChannel.id)
        } else if (channels.isNotEmpty()) {
            playChannel(channels.first().id)
        }
    }
    
    fun playPreviousChannel() {
        val channels = currentState.channels
        val currentIndex = currentState.currentIndex
        
        if (currentIndex > 0) {
            val prevChannel = channels[currentIndex - 1]
            playChannel(prevChannel.id)
        } else if (channels.isNotEmpty()) {
            playChannel(channels.last().id)
        }
    }
    
    fun stop() {
        positionUpdateJob?.cancel()
        setState {
            copy(
                currentChannel = null,
                isPlaying = false,
                position = 0,
                duration = 0
            )
        }
    }
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (currentState.isPlaying) {
                    setState { copy(position = position + 1000) }
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
    }
}
