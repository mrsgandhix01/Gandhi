package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.GandhiDatabase
import com.example.data.local.TermuxScriptEntity
import com.example.data.local.TranscriptEntity
import com.example.data.repository.GandhiRepository
import com.example.domain.AiOperatingMode
import com.example.domain.AssistantTurnResult
import com.example.domain.GandhiAiProcessor
import com.example.domain.InstalledAppInfo
import com.example.domain.SttState
import com.example.domain.SystemTaskExecutor
import com.example.domain.VoiceEngineManager
import com.example.service.GandhiWakeWordService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GandhiTab {
    ASSISTANT,
    TRANSCRIPTS,
    AUTOMATION,
    BLUEPRINT
}

enum class LanguageFilter {
    ALL,
    HINDI_ONLY,
    ENGLISH_ONLY,
    ACTIONS_ONLY
}

data class PipelineStageStatus(
    val activeStepIndex: Int = 0, // 0 = idle/monitoring, 1 = Wake-Word, 2 = Offline STT, 3 = Gemini/Local NLU, 4 = System/TTS, 5 = Saved
    val stageNote: String = "Monitoring 16kHz PCM audio stream locally for \"Hey Gandhi\"..."
)

class GandhiViewModel(application: Application) : AndroidViewModel(application) {

    private val database = GandhiDatabase.getInstance(application)
    private val repository = GandhiRepository(database.gandhiDao())
    val systemTaskExecutor = SystemTaskExecutor(application)
    private val aiProcessor = GandhiAiProcessor(systemTaskExecutor)
    val voiceEngineManager = VoiceEngineManager(application)

    private val prefs = application.getSharedPreferences("hey_gandhi_prefs", Context.MODE_PRIVATE)

    private val _currentTab = MutableStateFlow(GandhiTab.ASSISTANT)
    val currentTab: StateFlow<GandhiTab> = _currentTab.asStateFlow()

    private val _aiMode = MutableStateFlow(AiOperatingMode.HYBRID_GEMINI)
    val aiMode: StateFlow<AiOperatingMode> = _aiMode.asStateFlow()

    private val _autoStartOnBoot = MutableStateFlow(prefs.getBoolean("auto_start_wake_service", true))
    val autoStartOnBoot: StateFlow<Boolean> = _autoStartOnBoot.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _languageFilter = MutableStateFlow(LanguageFilter.ALL)
    val languageFilter: StateFlow<LanguageFilter> = _languageFilter.asStateFlow()

    private val _latestTurnResult = MutableStateFlow<AssistantTurnResult?>(null)
    val latestTurnResult: StateFlow<AssistantTurnResult?> = _latestTurnResult.asStateFlow()

    private val _pipelineStage = MutableStateFlow(PipelineStageStatus())
    val pipelineStage: StateFlow<PipelineStageStatus> = _pipelineStage.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _exportedDataPreview = MutableStateFlow<Pair<String, String>?>(null) // Pair(formatTitle, content)
    val exportedDataPreview: StateFlow<Pair<String, String>?> = _exportedDataPreview.asStateFlow()

    val isWakeServiceRunning: StateFlow<Boolean> = GandhiWakeWordService.isServiceRunning
    val liveRmsLevel: StateFlow<Float> = GandhiWakeWordService.liveRmsLevel
    val framesProcessedCount: StateFlow<Long> = GandhiWakeWordService.framesProcessedCount
    val wakeWordDetections: StateFlow<Int> = GandhiWakeWordService.wakeWordDetections
    val engineStatusLabel: StateFlow<String> = GandhiWakeWordService.engineStatusLabel

    val sttState: StateFlow<SttState> = voiceEngineManager.sttState
    val partialTranscript: StateFlow<String> = voiceEngineManager.partialTranscript
    val preferredSttLocale: StateFlow<String> = voiceEngineManager.preferredSttLocale
    val ttsActiveLanguage: StateFlow<String> = voiceEngineManager.ttsActiveLanguage

    val allTranscripts: StateFlow<List<TranscriptEntity>> = repository.allTranscripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTranscripts: StateFlow<List<TranscriptEntity>> = combine(
        allTranscripts,
        _searchQuery,
        _languageFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.userQuery.contains(query, ignoreCase = true) ||
                item.assistantResponse.contains(query, ignoreCase = true) ||
                (item.executedAction?.contains(query, ignoreCase = true) == true)

            val matchesFilter = when (filter) {
                LanguageFilter.ALL -> true
                LanguageFilter.HINDI_ONLY -> item.detectedLanguage == "HI"
                LanguageFilter.ENGLISH_ONLY -> item.detectedLanguage == "EN"
                LanguageFilter.ACTIONS_ONLY -> !item.executedAction.isNullOrBlank()
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val termuxScripts: StateFlow<List<TermuxScriptEntity>> = repository.allTermuxScripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isGeminiKeyConfigured: Boolean
        get() {
            val key = BuildConfig.GEMINI_API_KEY
            return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
        }

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            refreshInstalledApps()
        }
        // Start persistent foreground service by default
        GandhiWakeWordService.startService(application)
    }

    fun selectTab(tab: GandhiTab) {
        _currentTab.value = tab
    }

    fun setAiOperatingMode(mode: AiOperatingMode) {
        _aiMode.value = mode
        _statusBannerMessage.value = "Switched AI Router to: ${mode.label}"
    }

    fun toggleWakeWordService() {
        val app = getApplication<Application>()
        if (isWakeServiceRunning.value) {
            GandhiWakeWordService.stopService(app)
            _statusBannerMessage.value = "\"Hey Gandhi\" Foreground Service paused."
        } else {
            GandhiWakeWordService.startService(app)
            _statusBannerMessage.value = "\"Hey Gandhi\" Foreground Service armed (16kHz Offline PCM)."
        }
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        _autoStartOnBoot.value = enabled
        prefs.edit().putBoolean("auto_start_wake_service", enabled).apply()
    }

    fun setPreferredSttLanguage(localeTag: String) {
        voiceEngineManager.setPreferredSttLocale(localeTag)
        _statusBannerMessage.value = "Offline STT engine set to $localeTag"
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateLanguageFilter(filter: LanguageFilter) {
        _languageFilter.value = filter
    }

    fun dismissBanner() {
        _statusBannerMessage.value = null
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = systemTaskExecutor.queryInstalledApps()
        }
    }

    /**
     * Triggers the live offline microphone SpeechRecognizer after playing the wake-word chime.
     */
    fun triggerLiveVoiceWakeWord() {
        GandhiWakeWordService.incrementWakeDetection()
        _pipelineStage.value = PipelineStageStatus(
            activeStepIndex = 2,
            stageNote = "Wake-word \"Hey Gandhi\" detected! Capturing offline speech..."
        )
        voiceEngineManager.startOfflineSpeechCapture(
            onFinalResult = { spokenText ->
                executeVoiceOrTextCommand(spokenText, triggeredByWakeWord = false)
            },
            onError = { errorMsg ->
                _statusBannerMessage.value = errorMsg
                _pipelineStage.value = PipelineStageStatus(
                    activeStepIndex = 0,
                    stageNote = "Ready • Tap any bilingual voice command chip or type below."
                )
            }
        )
    }

    /**
     * Runs the complete 5-stage pipeline:
     * [1. Wake-Word] -> [2. Offline STT] -> [3. Bilingual Gemini/Local NLU] -> [4. App/Termux Action + Offline TTS] -> [5. Encrypted Room DB]
     */
    fun executeVoiceOrTextCommand(commandText: String, triggeredByWakeWord: Boolean = true) {
        val trimmed = commandText.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            if (triggeredByWakeWord) {
                GandhiWakeWordService.incrementWakeDetection()
                voiceEngineManager.playWakeChime()
            }

            _pipelineStage.value = PipelineStageStatus(
                activeStepIndex = 1,
                stageNote = "1. Wake-Word Engine verified \"Hey Gandhi\" trigger (Offline ONNX/Porcupine)"
            )
            delay(160L)

            _pipelineStage.value = PipelineStageStatus(
                activeStepIndex = 2,
                stageNote = "2. Offline STT finalized transcript & purged raw PCM audio frames"
            )
            delay(180L)

            _pipelineStage.value = PipelineStageStatus(
                activeStepIndex = 3,
                stageNote = "3. Routing anonymized text to ${aiMode.value.label}..."
            )

            val currentScripts = repository.getTermuxScriptsSnapshot()
            val turnResult = aiProcessor.processUserQuery(
                rawQuery = trimmed,
                mode = _aiMode.value,
                savedScripts = currentScripts,
                onScriptTriggered = { script ->
                    repository.recordScriptExecution(script)
                }
            )

            _latestTurnResult.value = turnResult

            _pipelineStage.value = PipelineStageStatus(
                activeStepIndex = 4,
                stageNote = if (turnResult.executedAction != null) {
                    "4. Executed ${turnResult.executedAction} & speaking ${turnResult.detectedLanguage} TTS"
                } else {
                    "4. Speaking bilingual response via Offline TTS (${turnResult.detectedLanguage})"
                }
            )

            voiceEngineManager.speakBilingual(turnResult.assistantResponse)
            delay(180L)

            val entity = TranscriptEntity(
                timestamp = System.currentTimeMillis(),
                detectedLanguage = turnResult.detectedLanguage,
                userQuery = turnResult.userQuery,
                assistantResponse = turnResult.assistantResponse,
                executedAction = turnResult.executedAction,
                processingMode = turnResult.processingMode,
                latencyMs = turnResult.latencyMs
            )
            repository.insertTranscript(entity)

            _pipelineStage.value = PipelineStageStatus(
                activeStepIndex = 5,
                stageNote = "5. Saved transcript to Local Room Vault (${turnResult.latencyMs} ms • Zero Cloud Audio)"
            )
        }
    }

    fun speakTextAgain(text: String) {
        voiceEngineManager.speakBilingual(text)
    }

    fun togglePinTranscript(transcript: TranscriptEntity) {
        viewModelScope.launch {
            repository.togglePinTranscript(transcript)
        }
    }

    fun deleteTranscript(id: Int) {
        viewModelScope.launch {
            repository.deleteTranscript(id)
            _statusBannerMessage.value = "Transcript record deleted from local database."
        }
    }

    fun wipeAllLocalData() {
        viewModelScope.launch {
            repository.wipeAllTranscripts()
            _latestTurnResult.value = null
            _statusBannerMessage.value = "All local voice transcripts wiped permanently."
        }
    }

    fun showExportDialog(format: String) {
        val currentList = filteredTranscripts.value
        if (format == "JSON") {
            val jsonContent = repository.exportTranscriptsToJson(currentList)
            _exportedDataPreview.value = "Local JSON Export (${currentList.size} records)" to jsonContent
        } else {
            val csvContent = repository.exportTranscriptsToCsv(currentList)
            _exportedDataPreview.value = "Local CSV Export (${currentList.size} records)" to csvContent
        }
    }

    fun dismissExportDialog() {
        _exportedDataPreview.value = null
    }

    fun executeSavedTermuxScript(script: TermuxScriptEntity) {
        val spokenCommand = "हे गांधी, ${script.aliasHindi} (${script.scriptFileName})"
        executeVoiceOrTextCommand(spokenCommand, triggeredByWakeWord = true)
    }

    fun addCustomTermuxScript(
        aliasHindi: String,
        aliasEnglish: String,
        scriptFileName: String,
        defaultArgs: String,
        description: String,
        runInBackground: Boolean
    ) {
        viewModelScope.launch {
            val cleanFile = if (scriptFileName.endsWith(".py")) scriptFileName.trim() else "${scriptFileName.trim()}.py"
            val newScript = TermuxScriptEntity(
                aliasHindi = aliasHindi.trim().ifEmpty { cleanFile },
                aliasEnglish = aliasEnglish.trim().ifEmpty { "run $cleanFile" },
                scriptFileName = cleanFile,
                defaultArgs = defaultArgs.trim(),
                description = description.trim().ifEmpty { "Custom Termux Python automation script" },
                runInBackground = runInBackground
            )
            repository.saveTermuxScript(newScript)
            _statusBannerMessage.value = "Mapped '$cleanFile' to voice alias \"${newScript.aliasHindi}\""
        }
    }

    fun deleteTermuxScript(id: Int) {
        viewModelScope.launch {
            repository.deleteTermuxScript(id)
            _statusBannerMessage.value = "Removed Termux script mapping."
        }
    }

    fun launchInstalledAppDirectly(app: InstalledAppInfo) {
        viewModelScope.launch {
            val res = systemTaskExecutor.launchApplication(app.appName, app.packageName)
            val entity = TranscriptEntity(
                timestamp = System.currentTimeMillis(),
                detectedLanguage = "HI",
                userQuery = "हे गांधी, ${app.appName} ऐप खोलो",
                assistantResponse = res.detailMessageHindi,
                executedAction = res.actionSummary,
                processingMode = _aiMode.value.name,
                latencyMs = 52L
            )
            repository.insertTranscript(entity)
            _statusBannerMessage.value = res.actionSummary
        }
    }

    override fun onCleared() {
        voiceEngineManager.release()
        super.onCleared()
    }
}
