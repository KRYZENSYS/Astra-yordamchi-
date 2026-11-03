package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CustomBlueprintEntity
import com.example.data.JarvisDatabase
import com.example.data.JarvisRepository
import com.example.data.MissionLogEntity
import com.example.data.cloud.AstrafolioCloudRepository
import com.example.data.cloud.AutoCallReplyLog
import com.example.data.cloud.JarvisCloudMission
import com.example.data.cloud.VoiceNoteItem
import com.example.device.JarvisDeviceController
import com.example.model.AiAgentRole
import com.example.model.AiDebateResponse
import com.example.model.CouncilMode
import com.example.model.CuratedJarvisBlueprints
import com.example.model.JarvisBlueprintIdea
import com.example.model.MultiAiCouncilResponse
import com.example.model.TacticalProtocol
import com.example.model.VisionAnalysisMode
import com.example.model.VisionHudResult
import com.example.model.VoicePersona
import com.example.network.AiStudioApiNode
import com.example.network.AiStudioMultiApiManager
import com.example.network.ApiTaskScope
import com.example.network.AstrafolioLocalHttpServer
import com.example.network.ClusterTokenSummary
import com.example.service.AstrafolioBackgroundService
import com.example.voice.JarvisVoiceManager
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

private const val TAG = "JarvisViewModel"

sealed interface CouncilUiState {
    data object Idle : CouncilUiState
    data class Orchestrating(val activePrompt: String) : CouncilUiState
    data class StreamingReply(val prompt: String, val partialText: String, val agent: AiAgentRole) : CouncilUiState
    data class CouncilSuccess(val response: MultiAiCouncilResponse) : CouncilUiState
    data class DebateSuccess(val debate: AiDebateResponse) : CouncilUiState
    data class StreamSuccess(val prompt: String, val fullText: String, val agent: AiAgentRole) : CouncilUiState
    data class Error(val message: String) : CouncilUiState
}

sealed interface VisionUiState {
    data object Idle : VisionUiState
    data class Analyzing(val mode: VisionAnalysisMode) : VisionUiState
    data class Success(val result: VisionHudResult) : VisionUiState
    data class Error(val message: String) : VisionUiState
}

sealed interface ProtocolUiState {
    data object Idle : ProtocolUiState
    data class Running(val protocol: TacticalProtocol) : ProtocolUiState
    data class ActiveReport(
        val protocol: TacticalProtocol,
        val spokenBriefing: String,
        val systemRecommendations: List<String>,
        val hardwareActionTaken: String
    ) : ProtocolUiState
    data class Error(val message: String) : ProtocolUiState
}

sealed interface BlueprintGenState {
    data object Idle : BlueprintGenState
    data class Generating(val topic: String) : BlueprintGenState
    data class Success(val blueprint: JarvisBlueprintIdea) : BlueprintGenState
    data class Error(val message: String) : BlueprintGenState
}

sealed interface AutoCallSecretaryUiState {
    data object Idle : AutoCallSecretaryUiState
    data class GeneratingReply(val callerNumber: String) : AutoCallSecretaryUiState
    data class ReplyReady(
        val callerNumber: String,
        val callerLabel: String,
        val spokenUzbekReply: String
    ) : AutoCallSecretaryUiState
    data class Error(val message: String) : AutoCallSecretaryUiState
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: JarvisRepository
    private val cloudRepository: AstrafolioCloudRepository
    val multiApiManager = AiStudioMultiApiManager(application)
    val voiceManager = JarvisVoiceManager(application)
    val deviceController = JarvisDeviceController(application)
    val localHttpServer = AstrafolioLocalHttpServer(
        context = application,
        multiApiManager = multiApiManager,
        deviceController = deviceController,
        onRemoteCommandReceived = { remoteCmd ->
            executeCommand(remoteCmd)
        }
    )

    init {
        val dao = JarvisDatabase.getInstance(application).jarvisDao()
        repository = JarvisRepository(dao)
        cloudRepository = AstrafolioCloudRepository(application)
        localHttpServer.startServer(8080)
    }

    // 1. Structured JSON Schema for 5-Agent Council + Full Voice Phone & Multi-API Control
    private val councilResponseSchema = Schema.obj(
        properties = mapOf(
            "detectedIntent" to Schema.string(description = "Buyruqning qisqa toifasi (masalan: Telefon Boshqaruvi, Qo'ng'iroq Kotibi, Lokal Server, Token Tahlili, Tizim Arxitekturasi)"),
            "deviceAction" to Schema.enumeration(
                values = listOf(
                    "NONE",
                    "TORCH_ON",
                    "TORCH_OFF",
                    "VOLUME_UP",
                    "VOLUME_DOWN",
                    "MUTE_AUDIO",
                    "MAX_AUDIO",
                    "SET_ALARM",
                    "SET_TIMER",
                    "DIAL_NUMBER",
                    "SEND_SMS",
                    "OPEN_CAMERA",
                    "OPEN_MAPS",
                    "OPEN_WIFI",
                    "OPEN_BLUETOOTH",
                    "OPEN_DISPLAY",
                    "OPEN_SOUND_SETTINGS",
                    "OPEN_LOCATION_SETTINGS",
                    "START_BACKGROUND_MODE",
                    "STOP_BACKGROUND_MODE",
                    "ENABLE_AUTO_CALL_RESPONDER",
                    "DISABLE_AUTO_CALL_RESPONDER",
                    "START_LOCAL_SERVER",
                    "STOP_LOCAL_SERVER",
                    "SLEEP_STANDBY_APIS",
                    "WAKE_ALL_APIS",
                    "ROTATE_API_NODE",
                    "SPEAK_TOKEN_REPORT",
                    "RESET_TOKEN_COUNTERS",
                    "WEB_SEARCH"
                ),
                description = "Agar foydalanuvchi telefon funksiyasini, AI API klasterini yoki token hisoblagichni boshqarishni so'rasa mos kodni tanla, aks holda NONE"
            ),
            "actionParameter" to Schema.string(description = "Telefon amali parametri: SET_ALARM uchun '07:30', VOLUME_UP/DOWN uchun '80', SET_TIMER uchun soniya '300', DIAL_NUMBER uchun telefon raqami, SEND_SMS uchun 'raqam|xabar', OPEN_MAPS uchun manzil, WEB_SEARCH uchun qidiruv so'zi, boshqa hollarda bo'sh"),
            "voiceSpokenSummary" to Schema.string(description = "JARVIS ovozda o'qib eshittiradigan 2-3 jumlalik ravon, tabiiy va kuchli o'zbek tilidagi javob xulosasi (bajarilgan telefon amalini ham tasdiqlasin)"),
            "architectReport" to Schema.string(description = "NEXUS-CODE (Texnik Arxitektor AI) tomonidan texnik yechim, dasturiy stek, API va kod arxitekturasi tavsifi (o'zbek tilida)"),
            "strategistPlan" to Schema.array(
                items = Schema.string(),
                description = "STRAT-MIND (Strategiya AIsi) tomonidan 3-5 qadamlik aniq amalga oshirish rejasi (o'zbek tilida)"
            ),
            "sentinelSecurityNote" to Schema.string(description = "AEGIS-GUARD (Xavfsizlik AIsi) tomonidan xavfsizlik, maxfiylik va orqa fonda barqaror ishlash bo'yicha muhim tavsiya (o'zbek tilida)"),
            "creativeSpark" to Schema.string(description = "VISION-NOVA (Innovatsiya AIsi) tomonidan loyihani yanada kuchliroq qiladigan futuristik g'oya (o'zbek tilida)"),
            "confidenceScore" to Schema.integer(description = "90 dan 100 gacha bo'lgan AI ishonchlilik ko'rsatkichi")
        )
    )

    // 2. Structured JSON Schema for Multi-AI Agent Debate Arena
    private val debateSchema = Schema.obj(
        properties = mapOf(
            "techThesis" to Schema.string(description = "NEXUS-CODE (Texnik Arxitektor AI) ning muhandislik va algoritmik nuqtai nazardan kuchli argumenti (o'zbek tilida)"),
            "strategyCounter" to Schema.string(description = "STRAT-MIND (Strategiya AIsi) ning qarshi-argumenti, uzoq muddatli xavflar va samaradorlik tahlili (o'zbek tilida)"),
            "securityAudit" to Schema.string(description = "AEGIS-GUARD (Xavfsizlik AIsi) ning kiberxavfsizlik va tizim barqarorligi bo'yicha keskin tanqidi va sharti (o'zbek tilida)"),
            "creativeBreakthrough" to Schema.string(description = "VISION-NOVA (Innovatsiya AIsi) ning barcha bahslarni birlashtiruvchi noan'anaviy kelajak yechimi (o'zbek tilida)"),
            "coreVerdict" to Schema.string(description = "JARVIS-CORE ning yakuniy hakamlik qarori — ovozda o'qiladigan 2-3 jumlalik aniq xulosa (o'zbek tilida)"),
            "winnerAgent" to Schema.string(description = "Bahsda eng kuchli yechim bergan agent nomi (masalan: NEXUS-CODE yoki STRAT-MIND)")
        )
    )

    // 3. Structured JSON Schema for Unified Multimodal Vision HUD
    private val visionSchema = Schema.obj(
        properties = mapOf(
            "detectedCategory" to Schema.string(description = "Rasmda aniqlangan asosiy obyekt, hujjat yoki kod turi (o'zbek tilida)"),
            "voiceSummary" to Schema.string(description = "JARVIS ovozda o'qib eshittiradigan 2-3 jumlalik aniq vizual diagnostika xulosasi (o'zbek tilida)"),
            "keyFindings" to Schema.array(
                items = Schema.string(),
                description = "Rasmda ko'ringan 3-4 ta eng muhim element, kod xatosi yoki hujjat bandlari (o'zbek tilida)"
            ),
            "technicalDeepDive" to Schema.string(description = "Chuqur muhandislik, kod yechimi yoki matnning batafsil tahlili (o'zbek tilida)"),
            "recommendedActions" to Schema.array(
                items = Schema.string(),
                description = "Foydalanuvchi bajarishi kerak bo'lgan 3 ta amaliy tavsiya (o'zbek tilida)"
            ),
            "confidenceScore" to Schema.integer(description = "90 dan 100 gacha aniqlik darajasi")
        )
    )

    // 4. Structured JSON Schema for Tactical Protocol & Telemetry Briefing
    private val protocolSchema = Schema.obj(
        properties = mapOf(
            "spokenBriefing" to Schema.string(description = "Telefonning real ko'rsatkichlari (batareya, harorat, RAM) va tanlangan protokol asosida JARVIS ovozda o'qiydigan 3-4 jumlalik kuchli taktik brifing (o'zbek tilida)"),
            "systemRecommendations" to Schema.array(
                items = Schema.string(),
                description = "Tizim unumdorligi, xavfsizlik va kunlik samaradorlik uchun 3 ta aniq harakat (o'zbek tilida)"
            )
        )
    )

    // 5. Structured JSON Schema for Custom Multi-AI Jarvis Blueprint Generator
    private val blueprintSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Yaratilgan Jarvis tizimining ta'sirchan nomi (o'zbek tilida)"),
            "category" to Schema.string(description = "Yo'nalish toifasi (masalan: Aqlli Uy, Dasturlash, Avto-Jarvis, Kiberxavfsizlik)"),
            "voicePipeline" to Schema.string(description = "Ovozli oqim zanjiri (masalan: Wake-Word -> Whisper STT -> Gemini Router -> Neural TTS)"),
            "aiModelsUsed" to Schema.array(
                items = Schema.string(),
                description = "Birgalikda ishlaydigan 3-4 ta sun'iy intellekt modellari va ularning vazifasi"
            ),
            "coreFeatures" to Schema.array(
                items = Schema.string(),
                description = "Tizimning 3-4 ta eng kuchli va noyob imkoniyatlari (o'zbek tilida)"
            ),
            "implementationSteps" to Schema.array(
                items = Schema.string(),
                description = "Ushbu Jarvis tizimini noldan qurish uchun 3-4 ta aniq muhandislik qadamlari (o'zbek tilida)"
            ),
            "powerLevel" to Schema.integer(description = "92 dan 100 gacha quvvat darajasi")
        )
    )

    // 6. Structured JSON Schema for AI Call Auto-Responder Secretary
    private val callSecretarySchema = Schema.obj(
        properties = mapOf(
            "spokenUzbekReply" to Schema.string(description = "Qo'ng'iroq qiluvchiga o'zbek tilida xushmuomalalik bilan aytiladigan va SMS qilib yuboriladigan 2-3 jumlalik mukammal avto-kotib javobi")
        )
    )

    private val streamingAgentModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val _councilUiState = MutableStateFlow<CouncilUiState>(CouncilUiState.Idle)
    val councilUiState: StateFlow<CouncilUiState> = _councilUiState.asStateFlow()

    private val _visionUiState = MutableStateFlow<VisionUiState>(VisionUiState.Idle)
    val visionUiState: StateFlow<VisionUiState> = _visionUiState.asStateFlow()

    private val _selectedVisionBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedVisionBitmap: StateFlow<Bitmap?> = _selectedVisionBitmap.asStateFlow()

    private val _protocolUiState = MutableStateFlow<ProtocolUiState>(ProtocolUiState.Idle)
    val protocolUiState: StateFlow<ProtocolUiState> = _protocolUiState.asStateFlow()

    private val _blueprintGenState = MutableStateFlow<BlueprintGenState>(BlueprintGenState.Idle)
    val blueprintGenState: StateFlow<BlueprintGenState> = _blueprintGenState.asStateFlow()

    private val _secretaryUiState = MutableStateFlow<AutoCallSecretaryUiState>(AutoCallSecretaryUiState.Idle)
    val secretaryUiState: StateFlow<AutoCallSecretaryUiState> = _secretaryUiState.asStateFlow()

    private val _selectedPersona = MutableStateFlow(VoicePersona.TACTICAL)
    val selectedPersona: StateFlow<VoicePersona> = _selectedPersona.asStateFlow()

    private val _councilMode = MutableStateFlow(CouncilMode.SWARM_AND_CONTROL)
    val councilMode: StateFlow<CouncilMode> = _councilMode.asStateFlow()

    private val _selectedAgentFilter = MutableStateFlow<AiAgentRole>(AiAgentRole.ARCHITECT)
    val selectedAgentFilter: StateFlow<AiAgentRole> = _selectedAgentFilter.asStateFlow()

    private val _autoSpeakEnabled = MutableStateFlow(true)
    val autoSpeakEnabled: StateFlow<Boolean> = _autoSpeakEnabled.asStateFlow()

    private val _pingingNodeId = MutableStateFlow<String?>(null)
    val pingingNodeId: StateFlow<String?> = _pingingNodeId.asStateFlow()

    val apiNodes: StateFlow<List<AiStudioApiNode>> = multiApiManager.apiNodes
    val clusterStatusBanner: StateFlow<String> = multiApiManager.clusterStatusBanner
    val tokenSummary: StateFlow<ClusterTokenSummary> = multiApiManager.tokenSummary
    val autoSleepIdleEnabled: StateFlow<Boolean> = multiApiManager.autoSleepIdleEnabled
    val autoSleepIdleMinutes: StateFlow<Int> = multiApiManager.autoSleepIdleMinutes
    val ecoSingleAwakeMode: StateFlow<Boolean> = multiApiManager.ecoSingleAwakeMode
    val autoFailoverEnabled: StateFlow<Boolean> = multiApiManager.autoFailoverEnabled
    val roundRobinKeysEnabled: StateFlow<Boolean> = multiApiManager.roundRobinKeysEnabled
    val localServerBridgeEnabled: StateFlow<Boolean> = multiApiManager.localServerBridgeEnabled
    val localServerEndpointUrl: StateFlow<String> = multiApiManager.localServerEndpointUrl

    val isEmbeddedServerRunning: StateFlow<Boolean> = localHttpServer.isServerRunning
    val embeddedServerAddress: StateFlow<String> = localHttpServer.localServerAddress
    val embeddedServerRequestsCount: StateFlow<Int> = localHttpServer.handledRequestsCount
    val lastEmbeddedServerEvent: StateFlow<String> = localHttpServer.lastServerEvent

    val savedMissions: StateFlow<List<MissionLogEntity>> = repository.allMissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudMissions: StateFlow<List<JarvisCloudMission>> = cloudRepository.observeMissions()
        .catch { e ->
            Log.w(TAG, "Cloud missions stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudCallLogs: StateFlow<List<AutoCallReplyLog>> = cloudRepository.observeCallLogs()
        .catch { e ->
            Log.w(TAG, "Cloud call logs stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudVoiceNotes: StateFlow<List<VoiceNoteItem>> = cloudRepository.observeVoiceNotes()
        .catch { e ->
            Log.w(TAG, "Cloud voice notes stream error", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBlueprints: StateFlow<List<JarvisBlueprintIdea>> = repository.allCustomBlueprints
        .map { customEntities ->
            val mappedCustom = customEntities.map { entity ->
                JarvisBlueprintIdea(
                    id = "custom_${entity.id}",
                    title = entity.title,
                    category = entity.category,
                    voicePipeline = entity.voicePipeline,
                    aiModelsUsed = entity.aiModelsJoined.split("||").filter { it.isNotBlank() },
                    coreFeatures = entity.coreFeaturesJoined.split("||").filter { it.isNotBlank() },
                    implementationSteps = entity.stepsJoined.split("||").filter { it.isNotBlank() },
                    powerLevel = entity.powerLevel
                )
            }
            mappedCustom + CuratedJarvisBlueprints.defaultBlueprints
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CuratedJarvisBlueprints.defaultBlueprints
        )

    fun setPersona(persona: VoicePersona) {
        _selectedPersona.value = persona
    }

    fun setCouncilMode(mode: CouncilMode) {
        _councilMode.value = mode
    }

    fun setAgentFilter(agent: AiAgentRole) {
        _selectedAgentFilter.value = agent
        _councilMode.value = CouncilMode.SINGLE_STREAM
    }

    fun setSelectedVisionBitmap(bitmap: Bitmap?) {
        _selectedVisionBitmap.value = bitmap
    }

    fun toggleAutoSpeak() {
        val next = !_autoSpeakEnabled.value
        _autoSpeakEnabled.value = next
        if (!next) {
            voiceManager.stopSpeaking()
        }
    }

    fun speakText(text: String) {
        voiceManager.speak(text, _selectedPersona.value)
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun toggleBackgroundService(enable: Boolean) {
        if (enable) {
            deviceController.startBackgroundMode()
        } else {
            deviceController.stopBackgroundMode()
        }
    }

    fun toggleAutoCallResponder(enable: Boolean) {
        deviceController.setAutoCallResponder(enable)
    }

    fun updateStatusPreset(preset: String) {
        AstrafolioBackgroundService.setStatusPreset(preset)
    }

    fun updateCustomReplyTemplate(template: String) {
        AstrafolioBackgroundService.setCustomReplyMessage(template)
    }

    fun addCustomApiNode(
        label: String,
        apiKey: String,
        modelName: String,
        scope: ApiTaskScope,
        startSleeping: Boolean,
        dailyTokenLimit: Long = 1_000_000L,
        serverUrl: String = ""
    ) {
        val added = multiApiManager.addCustomAiStudioNode(
            label = label,
            apiKey = apiKey,
            modelName = modelName,
            taskScope = scope,
            startSleeping = startSleeping,
            dailyTokenLimit = dailyTokenLimit,
            serverUrl = serverUrl
        )
        if (_autoSpeakEnabled.value) {
            voiceManager.speak("${added.label} klasterga ulandi. Holati: ${added.powerState.labelUz}", _selectedPersona.value)
        }
    }

    fun addMultipleApiKeysBatch(
        rawKeysInput: String,
        modelName: String,
        scope: ApiTaskScope,
        startSleeping: Boolean,
        dailyTokenLimit: Long = 1_000_000L,
        serverUrl: String = ""
    ) {
        val addedCount = multiApiManager.addMultipleApiKeysBatch(
            rawKeysInput = rawKeysInput,
            modelName = modelName,
            taskScope = scope,
            startSleeping = startSleeping,
            dailyTokenLimit = dailyTokenLimit,
            serverUrl = serverUrl
        )
        if (_autoSpeakEnabled.value && addedCount > 0) {
            voiceManager.speak(
                "$addedCount ta yangi AI Studio AQ. API kaliti klasterga ulandi va token hisoblagichga qo'shildi.",
                _selectedPersona.value
            )
        }
    }

    fun toggleEmbeddedLocalServer(enable: Boolean) {
        val msg = if (enable) {
            localHttpServer.startServer(8080)
        } else {
            localHttpServer.stopServer()
        }
        if (_autoSpeakEnabled.value && msg.isNotBlank()) {
            voiceManager.speak(msg, _selectedPersona.value)
        }
    }

    fun configureLocalServerBridge(enabled: Boolean, endpointUrl: String) {
        val msg = multiApiManager.configureLocalServerBridge(enabled, endpointUrl)
        if (_autoSpeakEnabled.value && msg.isNotBlank()) {
            voiceManager.speak(msg, _selectedPersona.value)
        }
    }

    fun updateNodeApiKeyAndLimit(
        nodeId: String,
        newApiKey: String,
        newDailyLimit: Long,
        newServerUrl: String? = null
    ) {
        val msg = multiApiManager.updateNodeApiKeyAndLimit(nodeId, newApiKey, newDailyLimit, newServerUrl)
        if (_autoSpeakEnabled.value && msg.isNotBlank()) {
            voiceManager.speak(msg, _selectedPersona.value)
        }
    }

    fun resetTokenCounters(nodeId: String? = null) {
        val msg = multiApiManager.resetTokenCounters(nodeId)
        if (_autoSpeakEnabled.value && msg.isNotBlank()) {
            voiceManager.speak(msg, _selectedPersona.value)
        }
    }

    fun speakTokenUsageReport() {
        val report = multiApiManager.buildSpokenTokenReport()
        voiceManager.speak(report, _selectedPersona.value)
    }

    fun toggleApiNodeSleep(nodeId: String) {
        val msg = multiApiManager.toggleNodeSleepState(nodeId)
        if (_autoSpeakEnabled.value && msg.isNotBlank()) {
            voiceManager.speak(msg, _selectedPersona.value)
        }
    }

    fun setPrimaryApiNode(nodeId: String) {
        val msg = multiApiManager.setPrimaryNode(nodeId)
        if (_autoSpeakEnabled.value && msg.isNotBlank()) {
            voiceManager.speak(msg, _selectedPersona.value)
        }
    }

    fun removeApiNode(nodeId: String) {
        multiApiManager.removeNode(nodeId)
    }

    fun sleepAllStandbyApis() {
        val msg = multiApiManager.sleepAllStandbyApis()
        if (_autoSpeakEnabled.value) {
            voiceManager.speak(msg, _selectedPersona.value)
        }
    }

    fun wakeAllApis() {
        val msg = multiApiManager.wakeAllApis()
        if (_autoSpeakEnabled.value) {
            voiceManager.speak(msg, _selectedPersona.value)
        }
    }

    fun rotateActiveApiNode() {
        val msg = multiApiManager.rotateToNextApiNode()
        if (_autoSpeakEnabled.value) {
            voiceManager.speak(msg, _selectedPersona.value)
        }
    }

    fun setAutoSleepIdle(enabled: Boolean, minutes: Int) {
        multiApiManager.setAutoSleepIdle(enabled, minutes)
    }

    fun setEcoSingleAwake(enabled: Boolean) {
        multiApiManager.setEcoSingleAwakeMode(enabled)
    }

    fun setAutoFailover(enabled: Boolean) {
        multiApiManager.setAutoFailoverEnabled(enabled)
    }

    fun setRoundRobinKeys(enabled: Boolean) {
        multiApiManager.setRoundRobinKeysEnabled(enabled)
    }

    fun pingApiNode(nodeId: String) {
        viewModelScope.launch {
            _pingingNodeId.value = nodeId
            try {
                val resultMsg = multiApiManager.pingNode(nodeId)
                if (_autoSpeakEnabled.value) {
                    voiceManager.speak(resultMsg, _selectedPersona.value)
                }
            } finally {
                _pingingNodeId.value = null
            }
        }
    }

    fun generateAndRespondToIncomingCall(callerNumber: String, callerLabel: String) {
        val cleanNumber = callerNumber.trim().ifBlank { "+998 90 123 45 67" }
        val cleanLabel = callerLabel.trim().ifBlank { "Kiruvchi qo'ng'iroq" }
        val statusPreset = AstrafolioBackgroundService.currentStatusPreset.value
        val customTemplate = AstrafolioBackgroundService.customReplyMessage.value
        val persona = _selectedPersona.value

        viewModelScope.launch {
            _secretaryUiState.value = AutoCallSecretaryUiState.GeneratingReply(cleanNumber)
            val prompt = """
                Sen 'ASTRAFOLIO AI KOTIB' (Jarvis Auto-Call Responder) tizimisan.
                Foydalanuvchiga hozir telefon qilishmoqda:
                - Qo'ng'iroq qiluvchi: "$cleanLabel" ($cleanNumber)
                - Xo'jayiningning hozirgi holati: "$statusPreset"
                - Qo'shimcha shablon eslatma: "$customTemplate"
                
                Qo'ng'iroq qiluvchiga o'zbek tilida juda xushmuomala, tabiiy va professional tarzda 2-3 jumlalik avto-javob matnini tuzib ber (go'yoki sen telefonni ko'tarib yoki darhol avto-javob qaytarib xo'jayining bandligini va tez orada aloqaga chiqishini tushuntiryapsan).
            """.trimIndent()

            try {
                val (jsonString, _) = withTimeout(60_000L) {
                    multiApiManager.generateJsonOrTextWithFailover(
                        scope = ApiTaskScope.AUTO_SECRETARY,
                        prompt = prompt,
                        schema = callSecretarySchema
                    )
                }
                val json = JSONObject(jsonString)
                val replyText = json.getString("spokenUzbekReply")

                _secretaryUiState.value = AutoCallSecretaryUiState.ReplyReady(
                    callerNumber = cleanNumber,
                    callerLabel = cleanLabel,
                    spokenUzbekReply = replyText
                )

                AstrafolioBackgroundService.recordAutoAnsweredCall("$cleanLabel ($cleanNumber): $replyText")

                if (_autoSpeakEnabled.value) {
                    voiceManager.speak(replyText, persona)
                }

                if (Firebase.auth.currentUser != null) {
                    cloudRepository.createCallLog(
                        callerNumber = cleanNumber,
                        callerLabel = cleanLabel,
                        statusPreset = statusPreset,
                        aiGeneratedReply = replyText,
                        autoAnswered = true
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Auto-call reply timed out", e)
                _secretaryUiState.value = AutoCallSecretaryUiState.Error("AI Kotib javob vaqti (60s) tugadi.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Auto-call reply failed", e)
                _secretaryUiState.value = AutoCallSecretaryUiState.Error(
                    e.localizedMessage ?: "Avtomatik javob yaratishda xatolik yuz berdi."
                )
            }
        }
    }

    fun createCloudVoiceNote(title: String, content: String) {
        if (Firebase.auth.currentUser == null) return
        viewModelScope.launch {
            cloudRepository.createVoiceNote(title, content)
        }
    }

    fun deleteCloudVoiceNote(noteId: String) {
        if (Firebase.auth.currentUser == null) return
        viewModelScope.launch {
            cloudRepository.deleteVoiceNote(noteId)
        }
    }

    fun deleteCloudCallLog(callId: String) {
        if (Firebase.auth.currentUser == null) return
        viewModelScope.launch {
            cloudRepository.deleteCallLog(callId)
        }
    }

    fun executeCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return

        when (_councilMode.value) {
            CouncilMode.SWARM_AND_CONTROL -> runMultiAgentCouncil(trimmed)
            CouncilMode.AI_DEBATE -> runMultiAgentDebate(trimmed)
            CouncilMode.SINGLE_STREAM -> runStreamingSingleAgent(trimmed, _selectedAgentFilter.value)
        }
    }

    private fun runMultiAgentCouncil(userCommand: String) {
        viewModelScope.launch {
            _councilUiState.value = CouncilUiState.Orchestrating(userCommand)
            // 0ms instant execution for unambiguous local hardware & API cluster commands
            val instantResult = deviceController.tryExecuteInstantHardwareCommand(userCommand)
            when (instantResult?.first) {
                "START_LOCAL_SERVER" -> localHttpServer.startServer(8080)
                "STOP_LOCAL_SERVER" -> localHttpServer.stopServer()
                "SLEEP_STANDBY_APIS" -> multiApiManager.sleepAllStandbyApis()
                "WAKE_ALL_APIS" -> multiApiManager.wakeAllApis()
                "ROTATE_API_NODE" -> multiApiManager.rotateToNextApiNode()
                "RESET_TOKEN_COUNTERS" -> multiApiManager.resetTokenCounters(null)
            }

            deviceController.refreshTelemetry()
            val tel = deviceController.telemetry.value
            val tok = multiApiManager.tokenSummary.value
            val persona = _selectedPersona.value

            val prompt = """
                Sen 'ASTRAFOLIO JARVIS' — 5 ta sun'iy intellekt agentidan iborat ovozli super-orkestrator, 24/7 orqa fon yordamchisi, AI Studio Multi-API Klaster boshqaruvchisi va telefon boshqaruvchisisan.
                Foydalanuvchi buyrug'i yoki savoli: "$userCommand"
                Telefonning hozirgi real telemetriyasi: Batareya ${tel.batteryPercent}%, Harorat ${tel.temperatureCelsius}°C, RAM ${tel.usedRamMb}/${tel.totalRamMb} MB.
                API Klaster Token Statistikasi: Jami ${tok.totalTokens} token ishlatilgan (Kiruvchi: ${tok.totalPromptTokens}, Chiquvchi: ${tok.totalOutputTokens}, So'rovlar: ${tok.totalRequestsCount} ta, Ulangan API kalitlar: ${tok.connectedKeysCount} ta).
                Ovoz va uslub rejimi: ${persona.labelUz} (${persona.promptInstruction}).
                
                MUHIM: Agar foydalanuvchi telefon funksiyasini, AI API klasterini yoki token hisoblagichni boshqarishni so'ragan bo'lsa:
                - Fonarik / chiroqni yoqish -> deviceAction = "TORCH_ON"
                - Fonarik / chiroqni o'chirish -> deviceAction = "TORCH_OFF"
                - Ovozni balandlatish yoki foizga qo'yish -> deviceAction = "VOLUME_UP", actionParameter = foiz raqami (masalan "80")
                - Ovozni pasaytirish -> deviceAction = "VOLUME_DOWN", actionParameter = foiz raqami (masalan "30")
                - Ovozni o'chirish (bezvuk) -> deviceAction = "MUTE_AUDIO"
                - Ovozni maksimal qilish -> deviceAction = "MAX_AUDIO"
                - Budilnik qo'yish -> deviceAction = "SET_ALARM", actionParameter = "HH:MM" (masalan "07:30")
                - Taymer qo'yish -> deviceAction = "SET_TIMER", actionParameter = soniyalar soni (masalan 5 daqiqa = "300")
                - Telefon qilish / raqam terish -> deviceAction = "DIAL_NUMBER", actionParameter = telefon raqami
                - SMS xabar yozish -> deviceAction = "SEND_SMS", actionParameter = "raqam|xabar matni"
                - Kamerani ochish -> deviceAction = "OPEN_CAMERA"
                - Xarita / navigatsiya ochish -> deviceAction = "OPEN_MAPS", actionParameter = manzil nomi
                - Wi-Fi sozlamalari -> deviceAction = "OPEN_WIFI"
                - Bluetooth sozlamalari -> deviceAction = "OPEN_BLUETOOTH"
                - Ekran / displey sozlamalari -> deviceAction = "OPEN_DISPLAY"
                - Ovoz sozlamalari -> deviceAction = "OPEN_SOUND_SETTINGS"
                - Joylashuv / GPS sozlamalari -> deviceAction = "OPEN_LOCATION_SETTINGS"
                - 24/7 Orqa fon rejimini yoqish -> deviceAction = "START_BACKGROUND_MODE"
                - Orqa fon rejimini to'xtatish -> deviceAction = "STOP_BACKGROUND_MODE"
                - Qo'ng'iroqlarga avto-javob (Avto-Kotib) rejimini yoqish -> deviceAction = "ENABLE_AUTO_CALL_RESPONDER"
                - Qo'ng'iroqlarga avto-javob rejimini o'chirish -> deviceAction = "DISABLE_AUTO_CALL_RESPONDER"
                - AI Studio API'larni uyqu (sleep) rejimiga o'tkazish -> deviceAction = "SLEEP_STANDBY_APIS"
                - Barcha AI Studio API'larni uyqudan uyg'otish -> deviceAction = "WAKE_ALL_APIS"
                - Keyingi AI Studio API uzeliga o'tish / almashtirish -> deviceAction = "ROTATE_API_NODE"
                - Token sarfi hisobotini aytish -> deviceAction = "SPEAK_TOKEN_REPORT"
                - Token hisoblagichni 0 ga tushirish / tozalash -> deviceAction = "RESET_TOKEN_COUNTERS"
                - Internetdan / Google-dan qidirish -> deviceAction = "WEB_SEARCH", actionParameter = qidiruv so'zi
                - Agar oddiy savol/topshiriq bo'lsa -> deviceAction = "NONE", actionParameter = ""
                
                Barcha maydonlarni 100% o'zbek tilida, juda aniq, professional va amaliy tarzda to'ldir.
            """.trimIndent()

            try {
                val (jsonString, usedApiNode) = withTimeout(60_000L) {
                    multiApiManager.generateJsonOrTextWithFailover(
                        scope = ApiTaskScope.VOICE_COUNCIL,
                        prompt = prompt,
                        schema = councilResponseSchema
                    )
                }
                val json = JSONObject(jsonString)

                val aiDeviceAction = json.optString("deviceAction", "NONE")
                val actionParam = json.optString("actionParameter", "")
                val apiClusterActionStatus = when (aiDeviceAction) {
                    "START_LOCAL_SERVER" -> localHttpServer.startServer(8080)
                    "STOP_LOCAL_SERVER" -> localHttpServer.stopServer()
                    "SLEEP_STANDBY_APIS" -> multiApiManager.sleepAllStandbyApis()
                    "WAKE_ALL_APIS" -> multiApiManager.wakeAllApis()
                    "ROTATE_API_NODE" -> multiApiManager.rotateToNextApiNode()
                    "SPEAK_TOKEN_REPORT" -> multiApiManager.buildSpokenTokenReport()
                    "RESET_TOKEN_COUNTERS" -> multiApiManager.resetTokenCounters(null)
                    else -> null
                }
                val executedStatus = apiClusterActionStatus
                    ?: if (instantResult != null && instantResult.first == aiDeviceAction) {
                        instantResult.second
                    } else {
                        deviceController.executeAiDeviceAction(aiDeviceAction, actionParam) ?: instantResult?.second
                    }
                val finalActionCode = if (aiDeviceAction != "NONE") aiDeviceAction else (instantResult?.first ?: "NONE")

                val stepsArray = json.getJSONArray("strategistPlan")
                val steps = (0 until stepsArray.length()).map { stepsArray.getString(it) }

                val latestTok = multiApiManager.tokenSummary.value
                val result = MultiAiCouncilResponse(
                    userCommand = userCommand,
                    detectedIntent = "${json.getString("detectedIntent")} • [${usedApiNode.label} • +${latestTok.lastRequestTokens} tok]",
                    deviceAction = finalActionCode,
                    actionParameter = actionParam,
                    executedActionStatus = executedStatus,
                    voiceSpokenSummary = json.getString("voiceSpokenSummary"),
                    architectReport = json.getString("architectReport"),
                    strategistPlan = steps,
                    sentinelSecurityNote = json.getString("sentinelSecurityNote"),
                    creativeSpark = json.getString("creativeSpark"),
                    confidenceScore = json.optInt("confidenceScore", 97).coerceIn(1, 100)
                )

                _councilUiState.value = CouncilUiState.CouncilSuccess(result)

                if (_autoSpeakEnabled.value) {
                    voiceManager.speak(result.voiceSpokenSummary, persona)
                }

                repository.saveMission(
                    MissionLogEntity(
                        commandText = userCommand,
                        detectedIntent = if (executedStatus != null) "${result.detectedIntent} [$finalActionCode]" else result.detectedIntent,
                        voiceSummary = result.voiceSpokenSummary,
                        architectReport = result.architectReport,
                        strategistPlanJoined = result.strategistPlan.joinToString("||"),
                        sentinelSecurityNote = result.sentinelSecurityNote,
                        creativeSpark = result.creativeSpark,
                        confidenceScore = result.confidenceScore
                    )
                )

                if (Firebase.auth.currentUser != null) {
                    cloudRepository.createMission(
                        prompt = userCommand,
                        mode = "VOICE_COUNCIL",
                        finalSpeech = result.voiceSpokenSummary,
                        architectReport = result.architectReport,
                        strategistPlan = result.strategistPlan.joinToString("\n"),
                        sentinelSecurityNote = result.sentinelSecurityNote,
                        creativeSpark = result.creativeSpark,
                        executedDeviceAction = executedStatus
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Multi-agent council timed out", e)
                _councilUiState.value = CouncilUiState.Error(
                    "Javob kutish vaqti (60 soniya) tugadi. Qaytadan urinib ko'ring."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Multi-agent council generation failed", e)
                _councilUiState.value = CouncilUiState.Error(
                    e.localizedMessage ?: "AI Kengashi tahlilida xatolik yuz berdi."
                )
            }
        }
    }

    private fun runMultiAgentDebate(topic: String) {
        viewModelScope.launch {
            _councilUiState.value = CouncilUiState.Orchestrating(topic)
            val persona = _selectedPersona.value
            val prompt = """
                Sen 'ASTRAFOLIO MULTI-AI DEBATE ARENA' — 4 ta mutaxassis sun'iy intellekt agentining ilmiy-texnik munozara maydonisan.
                Munozara mavzusi: "$topic"
                Har bir agent o'z ixtisosligi bo'yicha kuchli argument va qarshi-argument keltirsin, yakunda JARVIS-CORE hakam sifatida eng kuchli yechimni e'lon qilsin.
                Barcha matnlarni o'zbek tilida yoz.
            """.trimIndent()

            try {
                val (jsonString, _) = withTimeout(60_000L) {
                    multiApiManager.generateJsonOrTextWithFailover(
                        scope = ApiTaskScope.VOICE_COUNCIL,
                        prompt = prompt,
                        schema = debateSchema
                    )
                }
                val json = JSONObject(jsonString)

                val debate = AiDebateResponse(
                    topic = topic,
                    techThesis = json.getString("techThesis"),
                    strategyCounter = json.getString("strategyCounter"),
                    securityAudit = json.getString("securityAudit"),
                    creativeBreakthrough = json.getString("creativeBreakthrough"),
                    coreVerdict = json.getString("coreVerdict"),
                    winnerAgent = json.optString("winnerAgent", "JARVIS-CORE SYNTHESIS")
                )

                _councilUiState.value = CouncilUiState.DebateSuccess(debate)

                if (_autoSpeakEnabled.value) {
                    voiceManager.speak("AI Bahs Xulosasi: ${debate.coreVerdict}", persona)
                }

                repository.saveMission(
                    MissionLogEntity(
                        commandText = "[AI BAHS] $topic",
                        detectedIntent = "AI Munozara • G'olib: ${debate.winnerAgent}",
                        voiceSummary = debate.coreVerdict,
                        architectReport = debate.techThesis,
                        strategistPlanJoined = listOf(debate.strategyCounter, debate.creativeBreakthrough).joinToString("||"),
                        sentinelSecurityNote = debate.securityAudit,
                        creativeSpark = debate.creativeBreakthrough,
                        confidenceScore = 98
                    )
                )

                if (Firebase.auth.currentUser != null) {
                    cloudRepository.createMission(
                        prompt = topic,
                        mode = "AGENT_DEBATE",
                        finalSpeech = debate.coreVerdict,
                        architectReport = debate.techThesis,
                        strategistPlan = debate.strategyCounter,
                        sentinelSecurityNote = debate.securityAudit,
                        creativeSpark = debate.creativeBreakthrough,
                        executedDeviceAction = "G'olib: ${debate.winnerAgent}"
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Debate timed out", e)
                _councilUiState.value = CouncilUiState.Error("AI munozarasi vaqti (60s) tugadi.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Debate failed", e)
                _councilUiState.value = CouncilUiState.Error(
                    e.localizedMessage ?: "AI bahsini yaratishda xatolik yuz berdi."
                )
            }
        }
    }

    private fun runStreamingSingleAgent(userCommand: String, agent: AiAgentRole) {
        viewModelScope.launch {
            _councilUiState.value = CouncilUiState.Orchestrating(userCommand)
            val persona = _selectedPersona.value
            val accumulated = StringBuilder()
            val selectedNode = multiApiManager.acquireNodeForTask(ApiTaskScope.VOICE_COUNCIL)
            val startMs = System.currentTimeMillis()
            val prompt = """
                Sen ASTRAFOLIO JARVIS tizimining '${agent.codeName}' (${agent.titleUz}) maxsus AI agentisan.
                Vazifang: ${agent.specialtyUz}.
                Uslub: ${persona.promptInstruction}
                Foydalanuvchi murojaati: "$userCommand"
                Javobni o'zbek tilida, aniq bandlar bilan, amaliy va kuchli texnik/strategik tavsiyalar bilan yoz.
            """.trimIndent()

            try {
                withTimeout(60_000L) {
                    streamingAgentModel.generateContentStream(prompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulated.append(chunkText)
                        _councilUiState.value = CouncilUiState.StreamingReply(
                            prompt = userCommand,
                            partialText = accumulated.toString(),
                            agent = agent
                        )
                    }
                }
                val finalText = accumulated.toString()
                if (finalText.isNotBlank()) {
                    val latency = (System.currentTimeMillis() - startMs).coerceAtLeast(10L)
                    val pTok = multiApiManager.estimateTokenCount(prompt)
                    val oTok = multiApiManager.estimateTokenCount(finalText)
                    multiApiManager.recordNodeTelemetry(
                        nodeId = selectedNode.id,
                        latencyMs = latency,
                        statusNote = "Oqimli javob (${agent.codeName})",
                        promptTokensUsed = pTok,
                        outputTokensUsed = oTok,
                        totalTokensUsed = pTok + oTok
                    )
                    _councilUiState.value = CouncilUiState.StreamSuccess(
                        prompt = userCommand,
                        fullText = finalText,
                        agent = agent
                    )
                    if (_autoSpeakEnabled.value) {
                        val firstParagraph = finalText.lineSequence().firstOrNull { it.isNotBlank() } ?: finalText.take(220)
                        voiceManager.speak(firstParagraph, persona)
                    }
                } else {
                    _councilUiState.value = CouncilUiState.Error("Agent bo'sh javob qaytardi.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Streaming agent timed out", e)
                _councilUiState.value = CouncilUiState.Error("Oqimli javob vaqti (60s) tugadi.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Streaming agent failed", e)
                _councilUiState.value = CouncilUiState.Error(
                    e.localizedMessage ?: "Oqimli AI javobida xatolik yuz berdi."
                )
            }
        }
    }

    fun analyzeVisionImage(
        bitmap: Bitmap,
        mode: VisionAnalysisMode,
        customQuestion: String
    ) {
        _selectedVisionBitmap.value = bitmap
        viewModelScope.launch {
            _visionUiState.value = VisionUiState.Analyzing(mode)
            val persona = _selectedPersona.value
            val scaledBitmap = scaleBitmapDown(bitmap, 1280)

            val questionPart = if (customQuestion.isNotBlank()) {
                "Foydalanuvchining maxsus savoli: \"$customQuestion\"."
            } else {
                ""
            }

            val promptText = """
                Sen ASTRAFOLIO VISION-NOVA — JARVIS ning multimodal ko'rish, kod va hujjat tahlilchisi AIsisan.
                Tahlil rejimi: ${mode.labelUz} (${mode.promptHint}).
                $questionPart
                Ushbu tasvirni diqqat bilan tahlil qilib, barcha maydonlarni o'zbek tilida to'ldir.
            """.trimIndent()

            try {
                val (jsonString, _) = withTimeout(60_000L) {
                    multiApiManager.generateMultimodalWithTokens(
                        scope = ApiTaskScope.VISION_HUD,
                        multimodalContent = content {
                            image(scaledBitmap)
                            text(promptText)
                        },
                        promptTextForEstimate = promptText,
                        schema = visionSchema
                    )
                }
                val json = JSONObject(jsonString)

                val findingsArr = json.getJSONArray("keyFindings")
                val findings = (0 until findingsArr.length()).map { findingsArr.getString(it) }

                val actionsArr = json.getJSONArray("recommendedActions")
                val actions = (0 until actionsArr.length()).map { actionsArr.getString(it) }

                val result = VisionHudResult(
                    modeLabel = mode.labelUz,
                    detectedCategory = json.getString("detectedCategory"),
                    voiceSummary = json.getString("voiceSummary"),
                    keyFindings = findings,
                    technicalDeepDive = json.getString("technicalDeepDive"),
                    recommendedActions = actions,
                    confidenceScore = json.optInt("confidenceScore", 96).coerceIn(1, 100)
                )

                _visionUiState.value = VisionUiState.Success(result)

                if (_autoSpeakEnabled.value) {
                    voiceManager.speak(result.voiceSummary, persona)
                }

                repository.saveMission(
                    MissionLogEntity(
                        commandText = "[VISION HUD: ${mode.labelUz}] ${customQuestion.ifBlank { result.detectedCategory }}",
                        detectedIntent = "Vision: ${result.detectedCategory}",
                        voiceSummary = result.voiceSummary,
                        architectReport = result.technicalDeepDive,
                        strategistPlanJoined = result.recommendedActions.joinToString("||"),
                        sentinelSecurityNote = findings.firstOrNull().orEmpty(),
                        creativeSpark = findings.joinToString("; "),
                        confidenceScore = result.confidenceScore
                    )
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Vision HUD analysis timed out", e)
                _visionUiState.value = VisionUiState.Error("Tasvir tahlili vaqti (60s) tugadi.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Vision HUD analysis failed", e)
                _visionUiState.value = VisionUiState.Error(
                    e.localizedMessage ?: "Tasvirni tahlil qilishda xatolik yuz berdi."
                )
            }
        }
    }

    fun executeTacticalProtocol(protocol: TacticalProtocol) {
        viewModelScope.launch {
            _protocolUiState.value = ProtocolUiState.Running(protocol)
            deviceController.refreshTelemetry()
            val tel = deviceController.telemetry.value
            val persona = _selectedPersona.value

            val hardwareActionSummary = when (protocol) {
                TacticalProtocol.MORNING_BRIEFING -> {
                    deviceController.setMediaVolumePercent(75)
                    "Ovoz balandligi 75% brifing darajasiga moslandi"
                }
                TacticalProtocol.SYSTEM_DIAGNOSTICS -> {
                    deviceController.refreshTelemetry()
                    "Batareya (${tel.batteryPercent}%), Harorat (${tel.temperatureCelsius}°C) va RAM (${tel.ramUsagePercent}%) skanerlandi"
                }
                TacticalProtocol.DEEP_FOCUS -> {
                    deviceController.setTorchEnabled(false)
                    deviceController.setMediaVolumePercent(25)
                    "Chalg'ituvchi ovozlar 25% ga pasaytirildi, Fonarik o'chirildi"
                }
                TacticalProtocol.RED_ALERT -> {
                    deviceController.setTorchEnabled(true)
                    deviceController.setMediaVolumePercent(90)
                    "Fonarik taktik chirog'i YOQILDI va Ovoz 90% ga ko'tarildi"
                }
            }

            val freeGbFormatted = String.format(Locale.US, "%.1f", tel.freeStorageGb)
            val totalGbFormatted = String.format(Locale.US, "%.1f", tel.totalStorageGb)
            val gForceFormatted = String.format(Locale.US, "%.2f", tel.gForceMagnitude)

            val prompt = """
                Sen ASTRAFOLIO JARVIS tizimining avtonom protokol boshqaruvchisisan.
                Faollashtirilgan protokol: ${protocol.codeName} — ${protocol.titleUz} (${protocol.subtitleUz}).
                Bajarilgan apparat amali: $hardwareActionSummary.
                Qurilmaning hozirgi real ko'rsatkichlari:
                - Batareya: ${tel.batteryPercent}% (${if (tel.isCharging) "Quvvat olmoqda" else "Avtonom"}), Kuchlanish: ${tel.voltageMv} mV
                - Batareya harorati: ${tel.temperatureCelsius} °C
                - Operativ xotira (RAM): ${tel.usedRamMb} MB / ${tel.totalRamMb} MB (${tel.ramUsagePercent}% band)
                - Doimiy xotira: $freeGbFormatted GB bo'sh (Jami $totalGbFormatted GB)
                - G-Force datchigi: $gForceFormatted m/s²
                
                Ushbu real ko'rsatkichlarni tahlil qilib, o'zbek tilida haqiqiy JARVIS uslubida ovozli brifing va 3 ta aniq tavsiya ber.
            """.trimIndent()

            try {
                val (jsonString, _) = withTimeout(60_000L) {
                    multiApiManager.generateJsonOrTextWithFailover(
                        scope = ApiTaskScope.UNIVERSAL,
                        prompt = prompt,
                        schema = protocolSchema
                    )
                }
                val json = JSONObject(jsonString)

                val briefing = json.getString("spokenBriefing")
                val recsArr = json.getJSONArray("systemRecommendations")
                val recs = (0 until recsArr.length()).map { recsArr.getString(it) }

                _protocolUiState.value = ProtocolUiState.ActiveReport(
                    protocol = protocol,
                    spokenBriefing = briefing,
                    systemRecommendations = recs,
                    hardwareActionTaken = hardwareActionSummary
                )

                if (_autoSpeakEnabled.value) {
                    voiceManager.speak(briefing, persona)
                }

                if (Firebase.auth.currentUser != null) {
                    cloudRepository.createMission(
                        prompt = protocol.titleUz,
                        mode = "TACTICAL_PROTOCOL",
                        finalSpeech = briefing,
                        architectReport = hardwareActionSummary,
                        strategistPlan = recs.joinToString("\n"),
                        sentinelSecurityNote = "Batareya ${tel.batteryPercent}%, RAM ${tel.ramUsagePercent}%",
                        creativeSpark = protocol.codeName,
                        executedDeviceAction = hardwareActionSummary
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Protocol execution timed out", e)
                _protocolUiState.value = ProtocolUiState.Error("Protokol tahlili vaqti (60s) tugadi.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Protocol execution failed", e)
                _protocolUiState.value = ProtocolUiState.Error(
                    e.localizedMessage ?: "Protokolni ishga tushirishda xatolik."
                )
            }
        }
    }

    fun generateCustomJarvisBlueprint(topic: String) {
        val cleanTopic = topic.trim()
        if (cleanTopic.isBlank()) return

        viewModelScope.launch {
            _blueprintGenState.value = BlueprintGenState.Generating(cleanTopic)
            val prompt = """
                Foydalanuvchi quyidagi yo'nalishda mukammal, ovoz bilan va bir nechta sun'iy intellekt (Multi-AI) orqali ishlaydigan JARVIS yaratmoqchi:
                Mavzu / Yo'nalish: "$cleanTopic"
                Ushbu tizim uchun o'zbek tilida eng kuchli Multi-AI + Ovozli arxitektura loyihasini (Blueprint) ishlab chiq.
            """.trimIndent()

            try {
                val (jsonString, _) = withTimeout(60_000L) {
                    multiApiManager.generateJsonOrTextWithFailover(
                        scope = ApiTaskScope.UNIVERSAL,
                        prompt = prompt,
                        schema = blueprintSchema
                    )
                }
                val json = JSONObject(jsonString)

                val modelsArray = json.getJSONArray("aiModelsUsed")
                val models = (0 until modelsArray.length()).map { modelsArray.getString(it) }

                val featuresArray = json.getJSONArray("coreFeatures")
                val features = (0 until featuresArray.length()).map { featuresArray.getString(it) }

                val stepsArray = json.getJSONArray("implementationSteps")
                val steps = (0 until stepsArray.length()).map { stepsArray.getString(it) }

                val power = json.optInt("powerLevel", 98).coerceIn(90, 100)

                val blueprint = JarvisBlueprintIdea(
                    id = "gen_${System.currentTimeMillis()}",
                    title = json.getString("title"),
                    category = json.getString("category"),
                    voicePipeline = json.getString("voicePipeline"),
                    aiModelsUsed = models,
                    coreFeatures = features,
                    implementationSteps = steps,
                    powerLevel = power
                )

                repository.saveBlueprint(
                    CustomBlueprintEntity(
                        title = blueprint.title,
                        category = blueprint.category,
                        voicePipeline = blueprint.voicePipeline,
                        aiModelsJoined = models.joinToString("||"),
                        coreFeaturesJoined = features.joinToString("||"),
                        stepsJoined = steps.joinToString("||"),
                        powerLevel = power
                    )
                )

                _blueprintGenState.value = BlueprintGenState.Success(blueprint)

                if (_autoSpeakEnabled.value) {
                    voiceManager.speak(
                        "${blueprint.title} arxitekturasi tayyor. Ovoz zanjiri: ${blueprint.voicePipeline}",
                        _selectedPersona.value
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Blueprint generation timed out", e)
                _blueprintGenState.value = BlueprintGenState.Error("Arxitektura yaratish vaqti (60s) tugadi.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate custom Jarvis blueprint", e)
                _blueprintGenState.value = BlueprintGenState.Error(
                    e.localizedMessage ?: "Arxitektura generatsiyasida xatolik yuz berdi."
                )
            }
        }
    }

    fun deleteCustomBlueprint(blueprintId: String) {
        val dbId = blueprintId.removePrefix("custom_").toIntOrNull() ?: return
        viewModelScope.launch {
            repository.deleteBlueprint(dbId)
        }
    }

    fun deleteMission(id: Int) {
        viewModelScope.launch {
            repository.deleteMission(id)
        }
    }

    fun clearAllMissions() {
        viewModelScope.launch {
            repository.clearMissions()
        }
    }

    private fun scaleBitmapDown(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source
        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }
        return source.scale(targetWidth, targetHeight, true)
    }

    override fun onCleared() {
        super.onCleared()
        localHttpServer.stopServer()
        voiceManager.release()
        deviceController.release()
    }
}
