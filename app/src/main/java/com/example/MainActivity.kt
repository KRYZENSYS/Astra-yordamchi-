package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.automirrored.outlined.PhoneCallback
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.CouncilMode
import com.example.network.ApiNodePowerState
import com.example.service.AstrafolioBackgroundService
import com.example.ui.screens.AgentsMemoryScreen
import com.example.ui.screens.AiStudioClusterScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.AutoCallSecretaryScreen
import com.example.ui.screens.BlueprintsScreen
import com.example.ui.screens.TelemetryRadarScreen
import com.example.ui.screens.VisionHudScreen
import com.example.ui.screens.VoiceCouncilScreen
import com.example.ui.screens.signOutFromAstrafolio
import com.example.ui.theme.JarvisAmberGold
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDeepNavy
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.JarvisViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class JarvisDestination {
    VOICE_COUNCIL,
    AUTO_CALL_SECRETARY,
    VISION_HUD,
    TELEMETRY_RADAR,
    BLUEPRINTS_AND_MEMORY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge and render Auth-Gated UI
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AstrafolioRoot()
                }
            }
        }
    }
}

@Composable
fun AstrafolioRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        AstrafolioApp(
            currentUserId = currentUser?.uid.orEmpty()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstrafolioApp(
    currentUserId: String = "local_user",
    jarvisViewModel: JarvisViewModel = viewModel(key = currentUserId)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentTab by rememberSaveable { mutableStateOf(JarvisDestination.VOICE_COUNCIL) }
    var activeSubTabIndex by rememberSaveable { mutableStateOf(0) } // 0 = Blueprints, 1 = Memory, 2 = Multi-API Cluster

    val councilUiState by jarvisViewModel.councilUiState.collectAsStateWithLifecycle()
    val visionUiState by jarvisViewModel.visionUiState.collectAsStateWithLifecycle()
    val selectedVisionBitmap by jarvisViewModel.selectedVisionBitmap.collectAsStateWithLifecycle()
    val protocolUiState by jarvisViewModel.protocolUiState.collectAsStateWithLifecycle()
    val blueprintGenState by jarvisViewModel.blueprintGenState.collectAsStateWithLifecycle()
    val secretaryUiState by jarvisViewModel.secretaryUiState.collectAsStateWithLifecycle()
    val allBlueprints by jarvisViewModel.allBlueprints.collectAsStateWithLifecycle()
    val savedMissions by jarvisViewModel.savedMissions.collectAsStateWithLifecycle()
    val cloudCallLogs by jarvisViewModel.cloudCallLogs.collectAsStateWithLifecycle()
    val cloudVoiceNotes by jarvisViewModel.cloudVoiceNotes.collectAsStateWithLifecycle()
    val selectedPersona by jarvisViewModel.selectedPersona.collectAsStateWithLifecycle()
    val councilMode by jarvisViewModel.councilMode.collectAsStateWithLifecycle()
    val selectedAgentFilter by jarvisViewModel.selectedAgentFilter.collectAsStateWithLifecycle()
    val autoSpeakEnabled by jarvisViewModel.autoSpeakEnabled.collectAsStateWithLifecycle()

    val apiNodes by jarvisViewModel.apiNodes.collectAsStateWithLifecycle()
    val clusterStatusBanner by jarvisViewModel.clusterStatusBanner.collectAsStateWithLifecycle()
    val tokenSummary by jarvisViewModel.tokenSummary.collectAsStateWithLifecycle()
    val autoSleepIdleEnabled by jarvisViewModel.autoSleepIdleEnabled.collectAsStateWithLifecycle()
    val autoSleepIdleMinutes by jarvisViewModel.autoSleepIdleMinutes.collectAsStateWithLifecycle()
    val ecoSingleAwakeMode by jarvisViewModel.ecoSingleAwakeMode.collectAsStateWithLifecycle()
    val autoFailoverEnabled by jarvisViewModel.autoFailoverEnabled.collectAsStateWithLifecycle()
    val roundRobinKeysEnabled by jarvisViewModel.roundRobinKeysEnabled.collectAsStateWithLifecycle()
    val isEmbeddedServerRunning by jarvisViewModel.isEmbeddedServerRunning.collectAsStateWithLifecycle()
    val embeddedServerAddress by jarvisViewModel.embeddedServerAddress.collectAsStateWithLifecycle()
    val embeddedServerRequestsCount by jarvisViewModel.embeddedServerRequestsCount.collectAsStateWithLifecycle()
    val lastEmbeddedServerEvent by jarvisViewModel.lastEmbeddedServerEvent.collectAsStateWithLifecycle()
    val localServerBridgeEnabled by jarvisViewModel.localServerBridgeEnabled.collectAsStateWithLifecycle()
    val localServerEndpointUrl by jarvisViewModel.localServerEndpointUrl.collectAsStateWithLifecycle()
    val pingingNodeId by jarvisViewModel.pingingNodeId.collectAsStateWithLifecycle()

    val isListening by jarvisViewModel.voiceManager.isListening.collectAsStateWithLifecycle()
    val isSpeaking by jarvisViewModel.voiceManager.isSpeaking.collectAsStateWithLifecycle()
    val voiceRmsLevel by jarvisViewModel.voiceManager.voiceRmsLevel.collectAsStateWithLifecycle()
    val partialTranscript by jarvisViewModel.voiceManager.partialTranscript.collectAsStateWithLifecycle()
    val voiceStatusMessage by jarvisViewModel.voiceManager.voiceStatusMessage.collectAsStateWithLifecycle()

    val telemetry by jarvisViewModel.deviceController.telemetry.collectAsStateWithLifecycle()
    val isTorchOn by jarvisViewModel.deviceController.isTorchOn.collectAsStateWithLifecycle()
    val mediaVolumePercent by jarvisViewModel.deviceController.mediaVolumePercent.collectAsStateWithLifecycle()
    val lastExecutedDeviceAction by jarvisViewModel.deviceController.lastExecutedDeviceAction.collectAsStateWithLifecycle()

    val isBackgroundRunning by AstrafolioBackgroundService.isRunning.collectAsStateWithLifecycle()
    val autoCallEnabled by AstrafolioBackgroundService.autoCallResponderEnabled.collectAsStateWithLifecycle()
    val currentStatusPreset by AstrafolioBackgroundService.currentStatusPreset.collectAsStateWithLifecycle()
    val customReplyTemplate by AstrafolioBackgroundService.customReplyMessage.collectAsStateWithLifecycle()
    val screenedCallsCount by AstrafolioBackgroundService.screenedCallsCount.collectAsStateWithLifecycle()
    val lastScreenedCallInfo by AstrafolioBackgroundService.lastScreenedCallInfo.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = JarvisObsidian,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = JarvisDeepNavy
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_jarvis_hologram),
                            contentDescription = "Astrafolio Core Emblem",
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ASTRAFOLIO",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.hud_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisCyan
                            )
                        }
                    }
                },
                actions = {
                    val awakeApis = apiNodes.count { it.powerState == ApiNodePowerState.ACTIVE }
                    val formattedTok = jarvisViewModel.multiApiManager.formatTokenCount(tokenSummary.totalTokens)

                    Surface(
                        onClick = {
                            currentTab = JarvisDestination.BLUEPRINTS_AND_MEMORY
                            activeSubTabIndex = 2
                        },
                        color = JarvisSurfaceCard,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("top_bar_api_cluster_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = "AI Studio Multi-API Klaster",
                                tint = JarvisCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$awakeApis API • $formattedTok tok",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisCyan
                            )
                        }
                    }

                    Surface(
                        color = JarvisSurfaceCard,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isBackgroundRunning -> JarvisEmerald
                                            isTorchOn -> JarvisAmberGold
                                            else -> JarvisCyan
                                        }
                                    )
                            )
                            Text(
                                text = if (isBackgroundRunning) "24/7 FAOL" else "${telemetry.batteryPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isBackgroundRunning) JarvisEmerald else JarvisCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            signOutFromAstrafolio(
                                context = context,
                                onSignOutComplete = {},
                                scope = coroutineScope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Hisobdan chiqish",
                            tint = JarvisCrimson
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = JarvisDeepNavy,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == JarvisDestination.VOICE_COUNCIL,
                    onClick = { currentTab = JarvisDestination.VOICE_COUNCIL },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == JarvisDestination.VOICE_COUNCIL) {
                                Icons.Filled.GraphicEq
                            } else {
                                Icons.Outlined.GraphicEq
                            },
                            contentDescription = stringResource(R.string.tab_command)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_command)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisObsidian,
                        selectedTextColor = JarvisCyan,
                        indicatorColor = JarvisCyan
                    ),
                    modifier = Modifier.testTag("nav_tab_council")
                )

                NavigationBarItem(
                    selected = currentTab == JarvisDestination.AUTO_CALL_SECRETARY,
                    onClick = { currentTab = JarvisDestination.AUTO_CALL_SECRETARY },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == JarvisDestination.AUTO_CALL_SECRETARY) {
                                Icons.AutoMirrored.Filled.PhoneCallback
                            } else {
                                Icons.AutoMirrored.Outlined.PhoneCallback
                            },
                            contentDescription = stringResource(R.string.tab_secretary)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_secretary)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisObsidian,
                        selectedTextColor = JarvisCyan,
                        indicatorColor = JarvisCyan
                    ),
                    modifier = Modifier.testTag("nav_tab_secretary")
                )

                NavigationBarItem(
                    selected = currentTab == JarvisDestination.VISION_HUD,
                    onClick = { currentTab = JarvisDestination.VISION_HUD },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == JarvisDestination.VISION_HUD) {
                                Icons.Filled.Visibility
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = stringResource(R.string.tab_vision)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_vision)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisObsidian,
                        selectedTextColor = JarvisCyan,
                        indicatorColor = JarvisCyan
                    ),
                    modifier = Modifier.testTag("nav_tab_vision")
                )

                NavigationBarItem(
                    selected = currentTab == JarvisDestination.TELEMETRY_RADAR,
                    onClick = { currentTab = JarvisDestination.TELEMETRY_RADAR },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == JarvisDestination.TELEMETRY_RADAR) {
                                Icons.Filled.Radar
                            } else {
                                Icons.Outlined.Radar
                            },
                            contentDescription = stringResource(R.string.tab_radar)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_radar)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisObsidian,
                        selectedTextColor = JarvisCyan,
                        indicatorColor = JarvisCyan
                    ),
                    modifier = Modifier.testTag("nav_tab_radar")
                )

                NavigationBarItem(
                    selected = currentTab == JarvisDestination.BLUEPRINTS_AND_MEMORY,
                    onClick = { currentTab = JarvisDestination.BLUEPRINTS_AND_MEMORY },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == JarvisDestination.BLUEPRINTS_AND_MEMORY) {
                                Icons.Filled.AutoAwesome
                            } else {
                                Icons.Outlined.AutoAwesome
                            },
                            contentDescription = stringResource(R.string.tab_blueprints)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_blueprints)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisObsidian,
                        selectedTextColor = JarvisCyan,
                        indicatorColor = JarvisCyan
                    ),
                    modifier = Modifier.testTag("nav_tab_blueprints")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                JarvisDestination.VOICE_COUNCIL -> {
                    VoiceCouncilScreen(
                        uiState = councilUiState,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        voiceRmsLevel = voiceRmsLevel,
                        partialTranscript = partialTranscript,
                        voiceStatusMessage = voiceStatusMessage,
                        lastExecutedDeviceAction = lastExecutedDeviceAction,
                        selectedPersona = selectedPersona,
                        councilMode = councilMode,
                        selectedAgentFilter = selectedAgentFilter,
                        autoSpeakEnabled = autoSpeakEnabled,
                        onSelectPersona = jarvisViewModel::setPersona,
                        onSelectCouncilMode = jarvisViewModel::setCouncilMode,
                        onSelectAgentFilter = jarvisViewModel::setAgentFilter,
                        onToggleAutoSpeak = jarvisViewModel::toggleAutoSpeak,
                        onStartVoiceListening = {
                            jarvisViewModel.voiceManager.startListening { recognizedText ->
                                jarvisViewModel.executeCommand(recognizedText)
                            }
                        },
                        onStopVoiceListening = {
                            jarvisViewModel.voiceManager.stopListening()
                        },
                        onSpeakText = jarvisViewModel::speakText,
                        onStopSpeaking = jarvisViewModel::stopSpeaking,
                        onSendCommand = jarvisViewModel::executeCommand
                    )
                }

                JarvisDestination.AUTO_CALL_SECRETARY -> {
                    AutoCallSecretaryScreen(
                        isBackgroundRunning = isBackgroundRunning,
                        autoCallEnabled = autoCallEnabled,
                        currentStatusPreset = currentStatusPreset,
                        customReplyTemplate = customReplyTemplate,
                        screenedCallsCount = screenedCallsCount,
                        lastScreenedCallInfo = lastScreenedCallInfo,
                        secretaryUiState = secretaryUiState,
                        cloudCallLogs = cloudCallLogs,
                        cloudVoiceNotes = cloudVoiceNotes,
                        onToggleBackgroundService = jarvisViewModel::toggleBackgroundService,
                        onToggleAutoCallResponder = jarvisViewModel::toggleAutoCallResponder,
                        onSelectStatusPreset = jarvisViewModel::updateStatusPreset,
                        onUpdateCustomTemplate = jarvisViewModel::updateCustomReplyTemplate,
                        onGenerateAndRespondToCall = jarvisViewModel::generateAndRespondToIncomingCall,
                        onSendSmsReply = { num, msg ->
                            jarvisViewModel.deviceController.launchSmsComposer(num, msg)
                        },
                        onDeleteCallLog = jarvisViewModel::deleteCloudCallLog,
                        onCreateVoiceNote = jarvisViewModel::createCloudVoiceNote,
                        onDeleteVoiceNote = jarvisViewModel::deleteCloudVoiceNote,
                        onSpeakText = jarvisViewModel::speakText,
                        onNavigateBack = {
                            currentTab = JarvisDestination.VOICE_COUNCIL
                        }
                    )
                }

                JarvisDestination.VISION_HUD -> {
                    VisionHudScreen(
                        uiState = visionUiState,
                        selectedBitmap = selectedVisionBitmap,
                        onSelectBitmap = jarvisViewModel::setSelectedVisionBitmap,
                        onAnalyzeImage = jarvisViewModel::analyzeVisionImage,
                        onSpeakText = jarvisViewModel::speakText,
                        onNavigateBack = {
                            currentTab = JarvisDestination.VOICE_COUNCIL
                        }
                    )
                }

                JarvisDestination.TELEMETRY_RADAR -> {
                    TelemetryRadarScreen(
                        telemetry = telemetry,
                        isTorchOn = isTorchOn,
                        mediaVolumePercent = mediaVolumePercent,
                        protocolUiState = protocolUiState,
                        onRefreshTelemetry = { jarvisViewModel.deviceController.refreshTelemetry() },
                        onToggleTorch = { jarvisViewModel.deviceController.setTorchEnabled(it) },
                        onVolumeChange = { jarvisViewModel.deviceController.setMediaVolumePercent(it) },
                        onLaunchAlarm = { jarvisViewModel.deviceController.launchAlarmIntent("07:00") },
                        onLaunchTimer = { jarvisViewModel.deviceController.launchTimerIntent("300") },
                        onOpenSettings = { action, label ->
                            jarvisViewModel.deviceController.launchSystemSettings(action, label)
                        },
                        onExecuteProtocol = jarvisViewModel::executeTacticalProtocol,
                        onSpeakText = jarvisViewModel::speakText,
                        onNavigateBack = {
                            currentTab = JarvisDestination.VOICE_COUNCIL
                        }
                    )
                }

                JarvisDestination.BLUEPRINTS_AND_MEMORY -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = activeSubTabIndex == 0,
                                onClick = { activeSubTabIndex = 0 },
                                label = { Text("G'oyalar (${allBlueprints.size})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JarvisCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = JarvisCyan,
                                    selectedLeadingIconColor = JarvisCyan
                                ),
                                modifier = Modifier.testTag("subtab_blueprints")
                            )

                            FilterChip(
                                selected = activeSubTabIndex == 1,
                                onClick = { activeSubTabIndex = 1 },
                                label = { Text("5-AI Tarix (${savedMissions.size})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JarvisAmberGold.copy(alpha = 0.22f),
                                    selectedLabelColor = JarvisAmberGold,
                                    selectedLeadingIconColor = JarvisAmberGold
                                ),
                                modifier = Modifier.testTag("subtab_memory")
                            )

                            FilterChip(
                                selected = activeSubTabIndex == 2,
                                onClick = { activeSubTabIndex = 2 },
                                label = { Text("AI API (${apiNodes.size})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Hub,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JarvisEmerald.copy(alpha = 0.22f),
                                    selectedLabelColor = JarvisEmerald,
                                    selectedLeadingIconColor = JarvisEmerald
                                ),
                                modifier = Modifier.testTag("subtab_api_cluster")
                            )
                        }

                        when (activeSubTabIndex) {
                            0 -> {
                                BlueprintsScreen(
                                    blueprints = allBlueprints,
                                    genState = blueprintGenState,
                                    onGenerateBlueprint = jarvisViewModel::generateCustomJarvisBlueprint,
                                    onSpeakBlueprint = { bp ->
                                        val speech = "${bp.title}. Ovoz zanjiri: ${bp.voicePipeline}. Asosiy imkoniyat: ${bp.coreFeatures.firstOrNull().orEmpty()}"
                                        jarvisViewModel.speakText(speech)
                                    },
                                    onExploreInCouncil = { bp ->
                                        currentTab = JarvisDestination.VOICE_COUNCIL
                                        jarvisViewModel.setCouncilMode(CouncilMode.SWARM_AND_CONTROL)
                                        jarvisViewModel.executeCommand(
                                            "Ushbu '${bp.title}' tizimini qanday qilib amalda kod va datchiklar bilan yig'ish mumkin? Ovoz zanjiri: ${bp.voicePipeline}"
                                        )
                                    },
                                    onDeleteCustomBlueprint = jarvisViewModel::deleteCustomBlueprint,
                                    onNavigateBack = {
                                        currentTab = JarvisDestination.VOICE_COUNCIL
                                    }
                                )
                            }
                            1 -> {
                                AgentsMemoryScreen(
                                    missions = savedMissions,
                                    onSpeakText = jarvisViewModel::speakText,
                                    onSelectAgentForStream = { agent ->
                                        jarvisViewModel.setAgentFilter(agent)
                                        currentTab = JarvisDestination.VOICE_COUNCIL
                                    },
                                    onReplayMission = { cmd ->
                                        val cleanCmd = cmd.removePrefix("[AI BAHS] ").trim()
                                        currentTab = JarvisDestination.VOICE_COUNCIL
                                        jarvisViewModel.executeCommand(cleanCmd)
                                    },
                                    onDeleteMission = jarvisViewModel::deleteMission,
                                    onClearAllMissions = jarvisViewModel::clearAllMissions,
                                    onNavigateBack = {
                                        currentTab = JarvisDestination.VOICE_COUNCIL
                                    }
                                )
                            }
                            else -> {
                                AiStudioClusterScreen(
                                    apiNodes = apiNodes,
                                    clusterStatusBanner = clusterStatusBanner,
                                    tokenSummary = tokenSummary,
                                    autoSleepIdleEnabled = autoSleepIdleEnabled,
                                    autoSleepIdleMinutes = autoSleepIdleMinutes,
                                    ecoSingleAwakeMode = ecoSingleAwakeMode,
                                    autoFailoverEnabled = autoFailoverEnabled,
                                    roundRobinKeysEnabled = roundRobinKeysEnabled,
                                    isEmbeddedServerRunning = isEmbeddedServerRunning,
                                    embeddedServerAddress = embeddedServerAddress,
                                    embeddedServerRequestsCount = embeddedServerRequestsCount,
                                    lastEmbeddedServerEvent = lastEmbeddedServerEvent,
                                    localServerBridgeEnabled = localServerBridgeEnabled,
                                    localServerEndpointUrl = localServerEndpointUrl,
                                    pingingNodeId = pingingNodeId,
                                    onAddApiNode = jarvisViewModel::addCustomApiNode,
                                    onAddMultipleApiKeysBatch = jarvisViewModel::addMultipleApiKeysBatch,
                                    onUpdateNodeApiKeyAndLimit = jarvisViewModel::updateNodeApiKeyAndLimit,
                                    onToggleEmbeddedLocalServer = jarvisViewModel::toggleEmbeddedLocalServer,
                                    onConfigureLocalServerBridge = jarvisViewModel::configureLocalServerBridge,
                                    onResetTokenCounters = jarvisViewModel::resetTokenCounters,
                                    onSpeakTokenReport = jarvisViewModel::speakTokenUsageReport,
                                    onToggleNodeSleep = jarvisViewModel::toggleApiNodeSleep,
                                    onSetPrimaryNode = jarvisViewModel::setPrimaryApiNode,
                                    onPingNode = jarvisViewModel::pingApiNode,
                                    onRemoveNode = jarvisViewModel::removeApiNode,
                                    onSleepAllStandby = jarvisViewModel::sleepAllStandbyApis,
                                    onWakeAllApis = jarvisViewModel::wakeAllApis,
                                    onRotateActiveApi = jarvisViewModel::rotateActiveApiNode,
                                    onToggleAutoSleepIdle = jarvisViewModel::setAutoSleepIdle,
                                    onToggleEcoSingleAwake = jarvisViewModel::setEcoSingleAwake,
                                    onToggleAutoFailover = jarvisViewModel::setAutoFailover,
                                    onToggleRoundRobinKeys = jarvisViewModel::setRoundRobinKeys,
                                    onSpeakText = jarvisViewModel::speakText,
                                    onNavigateBack = {
                                        currentTab = JarvisDestination.VOICE_COUNCIL
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
