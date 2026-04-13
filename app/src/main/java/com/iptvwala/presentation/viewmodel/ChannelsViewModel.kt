package com.iptvwala.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.iptvwala.core.base.BaseViewModel
import com.iptvwala.core.base.UiEffect
import com.iptvwala.core.base.UiEvent
import com.iptvwala.core.base.UiState
import com.iptvwala.domain.model.Channel
import com.iptvwala.domain.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelsUiState(
    val isLoading: Boolean = true,
    val channels: List<Channel> = emptyList(),
    val favorites: List<Channel> = emptyList(),
    val groups: List<String> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null
) : UiState

sealed class ChannelsEvent : UiEvent {
    data class SearchQueryChanged(val query: String) : ChannelsEvent()
    data class ChannelClicked(val channel: Channel) : ChannelsEvent()
    data class ChannelFavoriteClicked(val channel: Channel) : ChannelsEvent()
    data class GroupSelected(val group: String) : ChannelsEvent()
}

sealed class ChannelsEffect : UiEffect {
    data class NavigateToPlayer(val channelId: Long) : ChannelsEffect()
    data class ShowToast(val message: String) : ChannelsEffect()
}

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : BaseViewModel<ChannelsUiState, ChannelsEvent, ChannelsEffect>() {
    
    init {
        loadChannels()
    }
    
    override fun createInitialState() = ChannelsUiState()
    
    override fun handleEvent(event: ChannelsEvent) {
        when (event) {
            is ChannelsEvent.SearchQueryChanged -> onSearchQueryChange(event.query)
            is ChannelsEvent.ChannelClicked -> onChannelClick(event.channel)
            is ChannelsEvent.ChannelFavoriteClicked -> toggleFavorite(event.channel)
            is ChannelsEvent.GroupSelected -> loadGroupChannels(event.group)
        }
    }
    
    private fun loadChannels() {
        viewModelScope.launch {
            channelRepository.getAllChannels().collect { channels ->
                setState { copy(isLoading = false, channels = channels) }
            }
        }
    }
    
    private fun loadGroupChannels(group: String) {
        viewModelScope.launch {
            channelRepository.getChannelsByGroup(group).collect { channels ->
                setState { copy(channels = channels) }
            }
        }
    }
    
    fun onSearchQueryChange(query: String) {
        setState { copy(searchQuery = query) }
        if (query.isBlank()) {
            loadChannels()
        } else {
            viewModelScope.launch {
                channelRepository.searchChannels(query).collect { results ->
                    setState { copy(channels = results) }
                }
            }
        }
    }
    
    private fun onChannelClick(channel: Channel) {
        viewModelScope.launch {
            channelRepository.setLastWatched(channel.id, System.currentTimeMillis())
        }
        setEffect(ChannelsEffect.NavigateToPlayer(channel.id))
    }
    
    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channel.id)
        }
    }
}
