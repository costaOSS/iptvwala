package com.iptvwala.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.iptvwala.core.base.BaseViewModel
import com.iptvwala.core.base.UiEffect
import com.iptvwala.core.base.UiEvent
import com.iptvwala.core.base.UiState
import com.iptvwala.domain.model.Channel
import com.iptvwala.domain.model.EpgProgram
import com.iptvwala.domain.repository.ChannelRepository
import com.iptvwala.domain.repository.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EpgUiState(
    val isLoading: Boolean = true,
    val channels: List<Channel> = emptyList(),
    val programs: Map<String, List<EpgProgram>> = emptyMap(),
    val currentPrograms: Map<String, EpgProgram?> = emptyMap(),
    val error: String? = null
) : UiState

sealed class EpgEvent : UiEvent {
    data class ChannelSelected(val channel: Channel) : EpgEvent()
    data class DateSelected(val date: Long) : EpgEvent()
    data class ProgramClicked(val program: EpgProgram) : EpgEvent()
    data object Refresh : EpgEvent()
}

sealed class EpgEffect : UiEffect {
    data class NavigateToPlayer(val channelId: Long) : EpgEffect()
    data class ShowToast(val message: String) : EpgEffect()
}

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository
) : BaseViewModel<EpgUiState, EpgEvent, EpgEffect>() {
    
    init {
        loadData()
    }
    
    override fun createInitialState() = EpgUiState()
    
    override fun handleEvent(event: EpgEvent) {
        when (event) {
            is EpgEvent.ChannelSelected -> loadChannelPrograms(event.channel)
            is EpgEvent.DateSelected -> loadProgramsForDate(event.date)
            is EpgEvent.ProgramClicked -> onProgramClick(event.program)
            EpgEvent.Refresh -> refresh()
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            channelRepository.getAllChannels().collect { channels ->
                setState { copy(channels = channels, isLoading = false) }
                
                channels.forEach { channel ->
                    loadCurrentProgram(channel)
                }
            }
        }
    }
    
    private fun loadChannelPrograms(channel: Channel) {
        val channelId = channel.tvgId ?: channel.id.toString()
        
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val startOfDay = getStartOfDay(now)
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000)
            
            epgRepository.getProgramsForChannel(channelId, startOfDay, endOfDay).collect { programs ->
                setState {
                    copy(programs = programs.toList().groupBy { channelId }.mapValues { it.value })
                }
            }
        }
    }
    
    private fun loadProgramsForDate(date: Long) {
        val startOfDay = getStartOfDay(date)
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000)
        
        viewModelScope.launch {
            epgRepository.getAllProgramsInRange(startOfDay, endOfDay).collect { programs ->
                setState {
                    copy(programs = programs.groupBy { it.channelId })
                }
            }
        }
    }
    
    private fun loadCurrentProgram(channel: Channel) {
        val channelId = channel.tvgId ?: channel.id.toString()
        
        viewModelScope.launch {
            val program = epgRepository.getCurrentProgram(channelId)
            setState {
                copy(currentPrograms = currentPrograms + (channelId to program))
            }
        }
    }
    
    private fun onProgramClick(program: EpgProgram) {
        viewModelScope.launch {
            val channel = currentState.channels.find { 
                it.tvgId == program.channelId || it.id.toString() == program.channelId 
            }
            channel?.let {
                setEffect(EpgEffect.NavigateToPlayer(it.id))
            }
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            try {
                epgRepository.refreshEpg()
            } catch (e: Exception) {
                setState { copy(error = e.message) }
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }
    
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
