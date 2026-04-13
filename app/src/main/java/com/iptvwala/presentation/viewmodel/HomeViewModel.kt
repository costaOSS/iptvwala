package com.iptvwala.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.iptvwala.core.base.BaseViewModel
import com.iptvwala.core.base.UiEffect
import com.iptvwala.core.base.UiEvent
import com.iptvwala.core.base.UiState
import com.iptvwala.domain.model.Channel
import com.iptvwala.domain.model.Source
import com.iptvwala.domain.repository.ChannelRepository
import com.iptvwala.domain.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val channels: List<Channel> = emptyList(),
    val recentlyWatched: List<Channel> = emptyList(),
    val favorites: List<Channel> = emptyList(),
    val groupedChannels: Map<String, List<Channel>> = emptyMap(),
    val featuredChannels: List<Channel> = emptyList(),
    val searchQuery: String = "",
    val filteredChannels: List<Channel> = emptyList(),
    val error: String? = null,
    val sources: List<Source> = emptyList()
) : UiState

sealed class HomeEvent : UiEvent {
    data class SearchQueryChanged(val query: String) : HomeEvent()
    data class ChannelClicked(val channel: Channel) : HomeEvent()
    data class ChannelFavoriteClicked(val channel: Channel) : HomeEvent()
    data object Refresh : HomeEvent()
    data object ClearError : HomeEvent()
}

sealed class HomeEffect : UiEffect {
    data class NavigateToPlayer(val channelId: Long) : HomeEffect()
    data class ShowToast(val message: String) : HomeEffect()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val sourceRepository: SourceRepository
) : BaseViewModel<HomeUiState, HomeEvent, HomeEffect>() {
    
    private var searchJob: Job? = null
    
    init {
        loadData()
    }
    
    override fun createInitialState() = HomeUiState()
    
    override fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SearchQueryChanged -> onSearchQueryChange(event.query)
            is HomeEvent.ChannelClicked -> onChannelClick(event.channel)
            is HomeEvent.ChannelFavoriteClicked -> toggleFavorite(event.channel)
            HomeEvent.Refresh -> refresh()
            HomeEvent.ClearError -> setState { copy(error = null) }
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            combine(
                channelRepository.getAllChannels(),
                channelRepository.getRecentlyWatched(),
                channelRepository.getFavoriteChannels(),
                sourceRepository.getAllSources()
            ) { channels, recent, favorites, sources ->
                Triple(
                    channels,
                    recent.take(10),
                    favorites
                ) to sources
            }.collect { (data, sources) ->
                val (channels, recent, favorites) = data
                val grouped = channels
                    .groupBy { it.groupTitle ?: "" }
                    .filterKeys { it.isNotEmpty() }
                    .toSortedMap()
                
                setState {
                    copy(
                        isLoading = false,
                        channels = channels,
                        recentlyWatched = recent,
                        favorites = favorites,
                        groupedChannels = grouped,
                        featuredChannels = channels.take(5),
                        sources = sources
                    )
                }
            }
        }
    }
    
    fun onSearchQueryChange(query: String) {
        setState { copy(searchQuery = query) }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            
            if (query.isBlank()) {
                setState { copy(filteredChannels = emptyList()) }
                return@launch
            }
            
            channelRepository.searchChannels(query).collect { results ->
                setState { copy(filteredChannels = results) }
            }
        }
    }
    
    private fun onChannelClick(channel: Channel) {
        setEffect(HomeEffect.NavigateToPlayer(channel.id))
    }
    
    private fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channel.id)
            val message = if (channel.isFavorite) "Removed from favorites" else "Added to favorites"
            setEffect(HomeEffect.ShowToast(message))
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            try {
                sourceRepository.getAllSources().first().forEach { source ->
                    sourceRepository.refreshSource(source.id)
                }
            } catch (e: Exception) {
                setState { copy(error = e.message) }
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }
}
