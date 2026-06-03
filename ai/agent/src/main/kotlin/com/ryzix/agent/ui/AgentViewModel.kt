package com.ryzix.agent.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ryzix.agent.context.ConversationHistory
import com.ryzix.agent.llm.GeminiProvider
import com.ryzix.agent.loop.AgentLoop
import com.ryzix.agent.tools.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class AgentViewModel : ViewModel() {

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var loop: AgentLoop? = null
    private val history = ConversationHistory()

    fun init(apiKey: String, projectRoot: File) {
        val llm = GeminiProvider(apiKey)
        val tools = listOf(
            FileReadTool(projectRoot),
            FileWriteTool(projectRoot),
            FileEditTool(projectRoot),
            TerminalTool(projectRoot),
            GrepTool(projectRoot),
            ListDirTool(projectRoot),
        )
        loop = AgentLoop(llm = llm, tools = tools, history = history, projectRoot = projectRoot)
    }

    fun sendMessage(message: String) {
        val currentLoop = loop ?: return
        if (_isRunning.value) return

        viewModelScope.launch {
            _isRunning.value = true
            currentLoop.run(message).collect { event ->
                when (event) {
                    is AgentLoop.Event.Thinking     -> _events.emit(UiEvent.ShowThinking(event.text))
                    is AgentLoop.Event.ToolCall     -> _events.emit(UiEvent.ShowToolCall(event.name, event.args))
                    is AgentLoop.Event.ToolResult   -> _events.emit(UiEvent.ShowToolResult(event.name, event.output.output, event.output.isError))
                    is AgentLoop.Event.FinalAnswer  -> _events.emit(UiEvent.ShowAnswer(event.text))
                    is AgentLoop.Event.AgentError   -> _events.emit(UiEvent.ShowError(event.message))
                }
            }
            _isRunning.value = false
        }
    }

    fun clearHistory() {
        history.clear()
        viewModelScope.launch { _events.emit(UiEvent.HistoryCleared) }
    }

    sealed class UiEvent {
        data class ShowThinking(val text: String)                                         : UiEvent()
        data class ShowToolCall(val name: String, val args: Map<String, String>)          : UiEvent()
        data class ShowToolResult(val name: String, val output: String, val isError: Boolean) : UiEvent()
        data class ShowAnswer(val text: String)                                           : UiEvent()
        data class ShowError(val message: String)                                         : UiEvent()
        object HistoryCleared                                                             : UiEvent()
    }
}
