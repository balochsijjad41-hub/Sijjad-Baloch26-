package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AiResult
import com.example.data.api.JarvisAiClient
import com.example.data.api.WeatherClient
import com.example.data.local.AppDatabase
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.model.AssistantState
import com.example.data.model.ChatMessage
import com.example.data.model.ConfirmationRequest
import com.example.data.model.JarvisMode
import com.example.data.model.JarvisTheme
import com.example.service.JarvisActionExecutor
import com.example.service.JarvisSpeechManager
import com.example.service.JarvisTtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveTimerState(
    val remainingSeconds: Long,
    val totalSeconds: Long,
    val label: String
)

data class JarvisUiState(
    val state: AssistantState = AssistantState.STANDBY,
    val audioLevel: Float = 0f,
    val messages: List<ChatMessage> = emptyList(),
    val livePartialText: String = "",
    val currentMode: JarvisMode = JarvisMode.NORMAL,
    val currentTheme: JarvisTheme = JarvisTheme.CYAN,
    val wakeWordEnabled: Boolean = false,
    val preferredLanguage: String = "ur-PK",
    val userName: String = "Sir",
    val speechRate: Float = 1.05f,
    val speechPitch: Float = 0.95f,
    val batteryPercent: Int = 85,
    val isCharging: Boolean = false,
    val confirmationRequest: ConfirmationRequest? = null,
    val activeTimer: ActiveTimerState? = null,
    val isCameraActive: Boolean = false,
    val isCheatSheetOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isServerMatrixOpen: Boolean = false,
    val isTextInputVisible: Boolean = false,
    val isTtsMuted: Boolean = false,
    val notes: List<NoteEntity> = emptyList(),
    val servers: List<com.example.data.model.ServerEndpoint> = emptyList(),
    val activeServer: com.example.data.model.ServerEndpoint? = null,
    val isTestingServers: Boolean = false,
    val autoSwitchEnabled: Boolean = true,
    val errorMessage: String? = null
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getInstance(context)
    private val aiClient = JarvisAiClient()
    private val weatherClient = WeatherClient()
    private val serverManager = com.example.service.JarvisServerManager()

    private val _uiState = MutableStateFlow(
        JarvisUiState(
            currentTheme = JarvisTheme.valueOf(prefs.getString("theme", JarvisTheme.CYAN.name) ?: JarvisTheme.CYAN.name),
            wakeWordEnabled = prefs.getBoolean("wake_word", false),
            preferredLanguage = prefs.getString("language", "ur-PK") ?: "ur-PK",
            userName = prefs.getString("user_name", "Sir") ?: "Sir",
            speechRate = prefs.getFloat("speech_rate", 1.05f),
            speechPitch = prefs.getFloat("speech_pitch", 0.95f),
            servers = serverManager.servers.value,
            activeServer = serverManager.activeServer.value,
            autoSwitchEnabled = serverManager.autoSwitchEnabled
        )
    )
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    private val actionExecutor = JarvisActionExecutor(
        context = context,
        scope = viewModelScope,
        database = database,
        weatherClient = weatherClient,
        onThemeChanged = { setTheme(it) },
        onModeChanged = { setMode(it) },
        onRequestConfirmation = { req -> _uiState.update { it.copy(confirmationRequest = req) } },
        onTimerTick = { rem, tot, lbl ->
            _uiState.update { it.copy(activeTimer = ActiveTimerState(rem, tot, lbl)) }
        },
        onTimerFinished = {
            _uiState.update { it.copy(activeTimer = null) }
        },
        onExecutionResult = { summary, speechText ->
            addMessage(ChatMessage.Sender.JARVIS, speechText, summary)
            speakOutLoud(speechText)
        }
    )

    private val speechManager = JarvisSpeechManager(
        context = context,
        onRmsChanged = { level ->
            if (_uiState.value.state == AssistantState.LISTENING) {
                _uiState.update { it.copy(audioLevel = level) }
            }
        },
        onPartialResult = { partial ->
            _uiState.update { it.copy(livePartialText = partial) }
        },
        onFinalResult = { text ->
            _uiState.update { it.copy(livePartialText = "") }
            processUserQuery(text)
        },
        onListeningStateChanged = { listening ->
            if (listening) {
                _uiState.update { it.copy(state = AssistantState.LISTENING, errorMessage = null) }
            } else if (_uiState.value.state == AssistantState.LISTENING) {
                _uiState.update { it.copy(state = AssistantState.STANDBY, audioLevel = 0f) }
            }
        },
        onError = { err ->
            _uiState.update { it.copy(errorMessage = err, state = AssistantState.ERROR, audioLevel = 0f) }
        }
    ).apply {
        preferredLocale = "ur-PK"
    }

    private val ttsManager = JarvisTtsManager(
        context = context,
        onStartSpeaking = {
            _uiState.update { it.copy(state = AssistantState.SPEAKING) }
        },
        onDoneSpeaking = {
            _uiState.update { it.copy(state = AssistantState.STANDBY, audioLevel = 0f) }
            // If continuous listening is enabled, resume listening after speaking
            if (speechManager.continuousMode) {
                speechManager.startListening()
            }
        },
        onError = {
            _uiState.update { it.copy(state = AssistantState.STANDBY) }
        }
    ).apply {
        speechRate = _uiState.value.speechRate
        speechPitch = _uiState.value.speechPitch
    }

    init {
        updateBatteryInfo()
        observeNotes()
        observeServers()

        // Welcome greeting on launch in Roman Urdu
        val initialUser = _uiState.value.userName
        addMessage(
            ChatMessage.Sender.JARVIS,
            "J.A.R.V.I.S. online hai, $initialUser. Tamaam systems fa'aal hain. Hukum farmaiye."
        )

        // Automatically benchmark and select best server on startup
        viewModelScope.launch {
            serverManager.benchmarkAllServers()
        }
    }

    private fun observeServers() {
        viewModelScope.launch {
            serverManager.servers.collect { sList ->
                _uiState.update { it.copy(servers = sList) }
            }
        }
        viewModelScope.launch {
            serverManager.activeServer.collect { active ->
                _uiState.update { it.copy(activeServer = active) }
            }
        }
        viewModelScope.launch {
            serverManager.isTestingServers.collect { testing ->
                _uiState.update { it.copy(isTestingServers = testing) }
            }
        }
    }

    private fun observeNotes() {
        viewModelScope.launch {
            database.noteDao().getAllNotesFlow().collect { noteList ->
                _uiState.update { it.copy(notes = noteList) }
            }
        }
    }

    fun toggleVoiceListening() {
        if (_uiState.value.state == AssistantState.LISTENING) {
            speechManager.stopListening()
            _uiState.update { it.copy(state = AssistantState.STANDBY, audioLevel = 0f) }
        } else {
            // Stop TTS if speaking (interruptible)
            ttsManager.stop()
            speechManager.startListening()
        }
    }

    fun processUserQuery(query: String, capturedImage: Bitmap? = null) {
        val trimmed = query.trim()
        if (trimmed.isBlank() && capturedImage == null) return

        // Interruption: stop current TTS speech
        ttsManager.stop()

        addMessage(ChatMessage.Sender.USER, trimmed)

        val lower = trimmed.lowercase()
        if (lower.contains("best server") || lower.contains("switch server") || lower.contains("server connect") ||
            lower.contains("server badlo") || lower.contains("server check") || lower.contains("fastest server")) {
            findBestServer()
            return
        }

        _uiState.update { it.copy(state = AssistantState.THINKING, errorMessage = null) }

        viewModelScope.launch {
            // Check memory context
            val memories = database.memoryDao().getAllMemory()
            val memoryContext = memories.joinToString("; ") { "${it.key}: ${it.value}" }

            val history = _uiState.value.messages.takeLast(6).map {
                (if (it.sender == ChatMessage.Sender.USER) "user" else "model") to it.text
            }

            val result = aiClient.generateResponse(
                userPrompt = trimmed,
                mode = _uiState.value.currentMode,
                userCustomName = _uiState.value.userName,
                conversationHistory = history,
                memoryContext = memoryContext,
                imageBitmap = capturedImage
            )

            when (result) {
                is AiResult.Success -> {
                    if (result.toolCall != null) {
                        actionExecutor.executeTool(
                            name = result.toolCall.name,
                            args = result.toolCall.args,
                            userCustomName = _uiState.value.userName
                        )
                    } else {
                        addMessage(ChatMessage.Sender.JARVIS, result.replyText)
                        speakOutLoud(result.replyText)
                    }
                }
                is AiResult.Error -> {
                    val fallbackMsg = "Sir, I encountered a communications latency. Systems standing by."
                    addMessage(ChatMessage.Sender.JARVIS, fallbackMsg)
                    speakOutLoud(fallbackMsg)
                    _uiState.update { it.copy(state = AssistantState.STANDBY, errorMessage = result.message) }
                }
            }
        }
    }

    fun speakOutLoud(text: String) {
        if (!_uiState.value.isTtsMuted) {
            ttsManager.speak(text)
        } else {
            _uiState.update { it.copy(state = AssistantState.STANDBY) }
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
        _uiState.update { it.copy(state = AssistantState.STANDBY) }
    }

    fun toggleTtsMute() {
        val newMute = !_uiState.value.isTtsMuted
        if (newMute) {
            ttsManager.stop()
        }
        _uiState.update { it.copy(isTtsMuted = newMute) }
    }

    fun toggleCamera(active: Boolean) {
        _uiState.update { it.copy(isCameraActive = active) }
    }

    fun toggleCheatSheet(open: Boolean) {
        _uiState.update { it.copy(isCheatSheetOpen = open) }
    }

    fun toggleSettings(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun toggleServerMatrix(open: Boolean) {
        _uiState.update { it.copy(isServerMatrixOpen = open) }
    }

    fun findBestServer() {
        viewModelScope.launch {
            _uiState.update { it.copy(state = AssistantState.THINKING) }
            val best = serverManager.benchmarkAllServers()
            val speech = "Sir, sab se behtareen server '${best.name}' ${best.latencyMs} millisecond latency ke sath connect kar diya gaya hai."
            val summary = "Switched to ${best.name} (${best.latencyMs}ms)"
            addMessage(ChatMessage.Sender.JARVIS, speech, summary)
            speakOutLoud(speech)
        }
    }

    fun toggleAutoSwitch(enabled: Boolean) {
        serverManager.autoSwitchEnabled = enabled
        _uiState.update { it.copy(autoSwitchEnabled = enabled) }
        if (enabled) {
            findBestServer()
        }
    }

    fun selectServer(server: com.example.data.model.ServerEndpoint) {
        serverManager.manuallySelectServer(server)
        val speech = "Sir, server '${server.name}' par switch kar diya hai."
        addMessage(ChatMessage.Sender.JARVIS, speech, "Active Server: ${server.name}")
        speakOutLoud(speech)
    }

    fun toggleTextInput(visible: Boolean) {
        _uiState.update { it.copy(isTextInputVisible = visible) }
    }

    fun clearConfirmation() {
        _uiState.update { it.copy(confirmationRequest = null) }
    }

    fun cancelActiveTimer() {
        actionExecutor.cancelActiveTimer()
        _uiState.update { it.copy(activeTimer = null) }
    }

    fun setMode(mode: JarvisMode) {
        _uiState.update { it.copy(currentMode = mode) }
    }

    fun setTheme(theme: JarvisTheme) {
        prefs.edit().putString("theme", theme.name).apply()
        _uiState.update { it.copy(currentTheme = theme) }
    }

    fun setWakeWord(enabled: Boolean) {
        prefs.edit().putBoolean("wake_word", enabled).apply()
        speechManager.wakeWordEnabled = enabled
        _uiState.update { it.copy(wakeWordEnabled = enabled) }
        if (enabled && !_uiState.value.isCameraActive) {
            speechManager.startListening()
        }
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        speechManager.preferredLocale = lang
        _uiState.update { it.copy(preferredLanguage = lang) }
    }

    fun setUserName(name: String) {
        val valid = name.ifBlank { "Sir" }
        prefs.edit().putString("user_name", valid).apply()
        _uiState.update { it.copy(userName = valid) }
    }

    fun setSpeechRate(rate: Float) {
        prefs.edit().putFloat("speech_rate", rate).apply()
        ttsManager.speechRate = rate
        _uiState.update { it.copy(speechRate = rate) }
    }

    fun setSpeechPitch(pitch: Float) {
        prefs.edit().putFloat("speech_pitch", pitch).apply()
        ttsManager.speechPitch = pitch
        _uiState.update { it.copy(speechPitch = pitch) }
    }

    fun clearAllData() {
        val confirmation = ConfirmationRequest(
            actionType = ConfirmationRequest.ActionType.DELETE_DATA,
            title = "Purge Memory Banks",
            description = "This will irreversibly delete all stored notes, to-do records, and user preferences.",
            payload = emptyMap(),
            onConfirm = {
                viewModelScope.launch {
                    database.noteDao().deleteAllNotes()
                    database.memoryDao().clearAllMemory()
                    _uiState.update { it.copy(confirmationRequest = null, messages = emptyList()) }
                    addMessage(ChatMessage.Sender.JARVIS, "All memory archives have been purged, Sir.")
                }
            },
            onCancel = {
                clearConfirmation()
            }
        )
        _uiState.update { it.copy(confirmationRequest = confirmation) }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            database.noteDao().deleteNote(note)
        }
    }

    fun toggleNoteComplete(note: NoteEntity) {
        viewModelScope.launch {
            database.noteDao().updateNote(note.copy(isCompleted = !note.isCompleted))
        }
    }

    private fun addMessage(sender: ChatMessage.Sender, text: String, actionSummary: String? = null) {
        val msg = ChatMessage(sender = sender, text = text, actionSummary = actionSummary)
        _uiState.update { it.copy(messages = it.messages + msg) }
    }

    private fun updateBatteryInfo() {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = bm.isCharging
            _uiState.update { it.copy(batteryPercent = level, isCharging = isCharging) }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
        actionExecutor.cancelActiveTimer()
    }
}
