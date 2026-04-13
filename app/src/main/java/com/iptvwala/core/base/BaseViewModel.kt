package com.iptvwala.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BaseViewModel<State, Event, Effect> : ViewModel() {
    
    private val initialState: State by lazy { createInitialState() }
    
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()
    
    private val _event = MutableSharedFlow<Event>()
    
    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect: Flow<Effect> = _effect.receiveAsFlow()
    
    val currentState: State
        get() = _state.value
    
    abstract fun createInitialState(): State
    
    init {
        subscribeToEvents()
    }
    
    private fun subscribeToEvents() {
        viewModelScope.launch {
            _event.collect { event ->
                handleEvent(event)
            }
        }
    }
    
    abstract fun handleEvent(event: Event)
    
    fun setEvent(event: Event) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }
    
    protected fun setState(reduce: State.() -> State) {
        _state.update { it.reduce() }
    }
    
    protected fun setEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}

interface UiState
interface UiEvent
interface UiEffect
