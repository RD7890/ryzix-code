package com.ryzix.agent.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.ryzix.agent.R
import kotlinx.coroutines.launch
import java.io.File

/**
 * The Ryzix agent panel — slide-in drawer on the right side of the IDE.
 * Shows the conversation, tool call trace, and the input field.
 */
class AgentPanelFragment : Fragment() {

    private val viewModel: AgentViewModel by viewModels()

    private lateinit var chatLog: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var clearButton: ImageButton
    private lateinit var thinkingIndicator: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_agent_panel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatLog          = view.findViewById(R.id.agent_chat_log)
        scrollView       = view.findViewById(R.id.agent_scroll)
        inputField       = view.findViewById(R.id.agent_input)
        sendButton       = view.findViewById(R.id.agent_send_btn)
        clearButton      = view.findViewById(R.id.agent_clear_btn)
        thinkingIndicator = view.findViewById(R.id.agent_thinking_indicator)

        // Init with API key from preferences/BuildConfig
        val apiKey = requireContext()
            .getSharedPreferences("ryzix_prefs", 0)
            .getString("gemini_api_key", "") ?: ""

        val projectRoot = File(requireContext().getExternalFilesDir(null), "projects/current")
        viewModel.init(apiKey, projectRoot)

        sendButton.setOnClickListener { sendMessage() }
        clearButton.setOnClickListener { viewModel.clearHistory() }

        inputField.setOnEditorActionListener { _, _, _ ->
            sendMessage(); true
        }

        lifecycleScope.launch {
            viewModel.events.collect { event -> handleEvent(event) }
        }

        lifecycleScope.launch {
            viewModel.isRunning.collect { running ->
                sendButton.isEnabled       = !running
                thinkingIndicator.visibility = if (running) View.VISIBLE else View.GONE
            }
        }
    }

    private fun sendMessage() {
        val text = inputField.text.toString().trim()
        if (text.isEmpty()) return
        inputField.text.clear()
        appendBubble(text, BubbleType.USER)
        viewModel.sendMessage(text)
    }

    private fun handleEvent(event: AgentViewModel.UiEvent) {
        when (event) {
            is AgentViewModel.UiEvent.ShowThinking   -> appendBubble("⚙ ${event.text}", BubbleType.SYSTEM)
            is AgentViewModel.UiEvent.ShowToolCall   -> appendBubble("🔧 ${event.name}(${event.args.entries.joinToString { "${it.key}=${it.value}" }})", BubbleType.TOOL)
            is AgentViewModel.UiEvent.ShowToolResult -> appendBubble(event.output, if (event.isError) BubbleType.ERROR else BubbleType.TOOL_RESULT)
            is AgentViewModel.UiEvent.ShowAnswer     -> appendBubble(event.text, BubbleType.ASSISTANT)
            is AgentViewModel.UiEvent.ShowError      -> appendBubble("❌ ${event.message}", BubbleType.ERROR)
            is AgentViewModel.UiEvent.HistoryCleared -> { chatLog.removeAllViews(); appendBubble("Conversation cleared.", BubbleType.SYSTEM) }
        }
    }

    private fun appendBubble(text: String, type: BubbleType) {
        val tv = TextView(requireContext()).apply {
            this.text = text
            textSize = 13f
            setPadding(24, 16, 24, 16)
            setBackgroundResource(type.backgroundRes)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(0, 4, 0, 4) }
        }
        chatLog.addView(tv)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    enum class BubbleType(val backgroundRes: Int) {
        USER(R.drawable.bg_bubble_user),
        ASSISTANT(R.drawable.bg_bubble_assistant),
        TOOL(R.drawable.bg_bubble_tool),
        TOOL_RESULT(R.drawable.bg_bubble_tool_result),
        SYSTEM(R.drawable.bg_bubble_system),
        ERROR(R.drawable.bg_bubble_error),
    }
}
