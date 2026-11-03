package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.AiAgentRole
import com.example.model.AiDebateResponse
import com.example.model.CouncilMode
import com.example.model.CuratedJarvisBlueprints
import com.example.model.MultiAiCouncilResponse
import com.example.model.VoicePersona
import com.example.ui.components.JarvisHudVisualizer
import com.example.ui.theme.JarvisAmberGold
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisViolet
import com.example.viewmodel.CouncilUiState

@Composable
fun VoiceCouncilScreen(
    uiState: CouncilUiState,
    isListening: Boolean,
    isSpeaking: Boolean,
    voiceRmsLevel: Float,
    partialTranscript: String,
    voiceStatusMessage: String?,
    lastExecutedDeviceAction: String?,
    selectedPersona: VoicePersona,
    councilMode: CouncilMode,
    selectedAgentFilter: AiAgentRole,
    autoSpeakEnabled: Boolean,
    onSelectPersona: (VoicePersona) -> Unit,
    onSelectCouncilMode: (CouncilMode) -> Unit,
    onSelectAgentFilter: (AiAgentRole) -> Unit,
    onToggleAutoSpeak: () -> Unit,
    onStartVoiceListening: () -> Unit,
    onStopVoiceListening: () -> Unit,
    onSpeakText: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onSendCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var commandInput by rememberSaveable { mutableStateOf("") }
    var micPermissionDeniedBanner by rememberSaveable { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micPermissionDeniedBanner = false
            onStartVoiceListening()
        } else {
            micPermissionDeniedBanner = true
        }
    }

    val isProcessing = uiState is CouncilUiState.Orchestrating || uiState is CouncilUiState.StreamingReply

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("voice_council_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. HUD Visualizer
        item {
            JarvisHudVisualizer(
                isListening = isListening,
                isSpeaking = isSpeaking,
                isProcessing = isProcessing,
                voiceRmsLevel = voiceRmsLevel,
                selectedAgent = if (councilMode == CouncilMode.SINGLE_STREAM) selectedAgentFilter else null
            )
        }

        // 2. Voice Action Row (Mic Button + Auto-Speak Toggle + Stop TTS)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (isListening) {
                            onStopVoiceListening()
                        } else {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                onStartVoiceListening()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) JarvisCrimson else JarvisCyan,
                        contentColor = if (isListening) Color.White else Color(0xFF001F29)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("voice_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = stringResource(R.string.btn_voice_listen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) stringResource(R.string.btn_voice_stop)
                        else stringResource(R.string.btn_voice_listen),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onToggleAutoSpeak,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (autoSpeakEnabled) JarvisCyan else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("toggle_autospeak_button")
                ) {
                    Icon(
                        imageVector = if (autoSpeakEnabled) Icons.AutoMirrored.Filled.VolumeUp
                        else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Auto Speak Toggle",
                        tint = if (autoSpeakEnabled) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (autoSpeakEnabled) "TTS ON" else "TTS OFF",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (autoSpeakEnabled) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSpeaking) {
                    IconButton(
                        onClick = onStopSpeaking,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(JarvisAmberGold.copy(alpha = 0.2f))
                            .testTag("stop_tts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop speaking",
                            tint = JarvisAmberGold
                        )
                    }
                }
            }
        }

        // Executed Phone Action Banner
        if (!lastExecutedDeviceAction.isNullOrBlank()) {
            item {
                Surface(
                    color = JarvisEmerald.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, JarvisEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_action_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = JarvisEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AVTONOM TELEFON BOSHQARUVI BAJARILDI:",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisEmerald
                            )
                            Text(
                                text = lastExecutedDeviceAction,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Live Partial Transcript or Permission / Status Notice
        if (partialTranscript.isNotBlank() || !voiceStatusMessage.isNullOrBlank() || micPermissionDeniedBanner) {
            item {
                Surface(
                    color = JarvisSurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (partialTranscript.isNotBlank()) {
                            Text(
                                text = "Jonli ovoz: \"$partialTranscript\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = JarvisCyan
                            )
                        }
                        if (!voiceStatusMessage.isNullOrBlank()) {
                            Text(
                                text = voiceStatusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (micPermissionDeniedBanner) {
                            Text(
                                text = stringResource(R.string.permission_mic_rationale),
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisAmberGold
                            )
                        }
                    }
                }
            }
        }

        // 3. Council Orchestration Mode & Voice Persona Selectors
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ISHLASH REJIMI (BOSHQARUV / AI BAHS / STREAM)",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisCyan
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CouncilMode.entries.forEach { mode ->
                        val selected = mode == councilMode
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCouncilMode(mode) },
                            label = { Text(mode.labelUz) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (mode) {
                                        CouncilMode.SWARM_AND_CONTROL -> Icons.Default.PhoneAndroid
                                        CouncilMode.AI_DEBATE -> Icons.Default.Forum
                                        CouncilMode.SINGLE_STREAM -> Icons.Default.Bolt
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisCyan.copy(alpha = 0.22f),
                                selectedLabelColor = JarvisCyan,
                                selectedLeadingIconColor = JarvisCyan
                            ),
                            modifier = Modifier.testTag("council_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                if (councilMode == CouncilMode.SINGLE_STREAM) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiAgentRole.entries.forEach { agent ->
                            val isSelected = selectedAgentFilter == agent
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectAgentFilter(agent) },
                                label = { Text(agent.codeName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = agent.accentColor.copy(alpha = 0.22f),
                                    selectedLabelColor = agent.accentColor
                                ),
                                modifier = Modifier.testTag("agent_filter_${agent.name.lowercase()}")
                            )
                        }
                    }
                }

                Text(
                    text = "OVOZ OHANGI (VOICE PERSONA)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoicePersona.entries.forEach { persona ->
                        val selected = persona == selectedPersona
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectPersona(persona) },
                            label = { Text(persona.labelUz) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisAmberGold.copy(alpha = 0.2f),
                                selectedLabelColor = JarvisAmberGold,
                                selectedLeadingIconColor = JarvisAmberGold
                            ),
                            modifier = Modifier.testTag("persona_chip_${persona.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // 4. Command Input Box + Mode-Specific Presets
        item {
            val activePresets = if (councilMode == CouncilMode.AI_DEBATE) {
                CuratedJarvisBlueprints.quickDebateTopics
            } else {
                CuratedJarvisBlueprints.quickVoiceMissions
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.placeholder_command),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (commandInput.isNotBlank()) {
                                        onSendCommand(commandInput)
                                    }
                                },
                                enabled = commandInput.isNotBlank() && !isProcessing,
                                modifier = Modifier.testTag("send_command_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.btn_send_command),
                                    tint = if (commandInput.isNotBlank()) JarvisCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("command_input_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (councilMode == CouncilMode.AI_DEBATE) {
                            "TEZKOR AI MUNOZARA MAVZULARI (4 TA AGENT BAHSI):"
                        } else {
                            "TEZKOR OVOZLI TELEFON BOSHQARUVI VA KENGASH BUYRUQLARI:"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisAmberGold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        activePresets.forEachIndexed { idx, preset ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.22f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isProcessing) {
                                        commandInput = preset
                                        onSendCommand(preset)
                                    }
                                    .testTag("quick_mission_$idx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (councilMode == CouncilMode.AI_DEBATE) Icons.Default.Forum else Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = JarvisCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Dynamic Output Section based on CouncilUiState
        when (uiState) {
            is CouncilUiState.Idle -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Astrafolio Ovozli Boshqaruv va 5-AI Kengashi Tayyor",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ovozda 'Fonarikni yoq', 'Ovozni 80% ga ko'tar', 'Soat 07:30 ga budilnik qo'y' deb telefonni boshqaring yoki murakkab muhandislik savollarini 5 ta AI agent kengashiga va bahs maydoniga tashlang.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            is CouncilUiState.Orchestrating -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, JarvisAmberGold)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = JarvisAmberGold,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "AI AGENTLAR HAMKORLIKDA ISHLAMOQDA...",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = JarvisAmberGold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${uiState.activePrompt}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            is CouncilUiState.StreamingReply -> {
                item {
                    StreamingAgentCard(
                        agent = uiState.agent,
                        prompt = uiState.prompt,
                        text = uiState.partialText,
                        isStreaming = true,
                        onSpeak = { onSpeakText(uiState.partialText) }
                    )
                }
            }

            is CouncilUiState.StreamSuccess -> {
                item {
                    StreamingAgentCard(
                        agent = uiState.agent,
                        prompt = uiState.prompt,
                        text = uiState.fullText,
                        isStreaming = false,
                        onSpeak = { onSpeakText(uiState.fullText) }
                    )
                }
            }

            is CouncilUiState.DebateSuccess -> {
                item {
                    MultiAiDebateResultSection(
                        debate = uiState.debate,
                        onSpeakVerdict = { onSpeakText(uiState.debate.coreVerdict) },
                        onSpeakFullDebate = {
                            val full = "Mavzu: ${uiState.debate.topic}. Texnik tezis: ${uiState.debate.techThesis}. Strategik qarshi fikr: ${uiState.debate.strategyCounter}. Yakuniy qaror: ${uiState.debate.coreVerdict}"
                            onSpeakText(full)
                        }
                    )
                }
            }

            is CouncilUiState.CouncilSuccess -> {
                item {
                    MultiAiCouncilResultSection(
                        response = uiState.response,
                        onSpeakSummary = { onSpeakText(uiState.response.voiceSpokenSummary) },
                        onSpeakFull = {
                            val combined = "${uiState.response.voiceSpokenSummary}. Texnik arxitektura: ${uiState.response.architectReport}. Innovatsion g'oya: ${uiState.response.creativeSpark}"
                            onSpeakText(combined)
                        }
                    )
                }
            }

            is CouncilUiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = JarvisCrimson.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, JarvisCrimson),
                        modifier = Modifier.testTag("council_error_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "TIZIM XATOLIGI",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisCrimson
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiAiDebateResultSection(
    debate: AiDebateResponse,
    onSpeakVerdict: () -> Unit,
    onSpeakFullDebate: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag("debate_result_section")
    ) {
        // 1. JARVIS-CORE Final Judge Verdict
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, JarvisCyan)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = JarvisCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "JARVIS-CORE // HAKAM XULOSASI",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisCyan
                            )
                            Text(
                                text = "Eng kuchli pozitsiya: ${debate.winnerAgent}",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisEmerald
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSpeakVerdict,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, JarvisCyan)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("O'qish", style = MaterialTheme.typography.labelSmall, color = JarvisCyan)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = debate.coreVerdict,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onSpeakFullDebate,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, JarvisElectricBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = JarvisElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Butun Munozarani Ovozda Tinglash",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisElectricBlue
                    )
                }
            }
        }

        // 2. Round 1: NEXUS-CODE Thesis
        AgentReportCard(
            codeName = "1-RAUND: NEXUS-CODE (TEXNIK TEZIS)",
            subtitle = "Muhandislik va algoritmik ustunlik dalili",
            icon = Icons.Default.Code,
            accentColor = JarvisElectricBlue,
            content = debate.techThesis
        )

        // 3. Round 2: STRAT-MIND Counter-Argument
        AgentReportCard(
            codeName = "2-RAUND: STRAT-MIND (QARSHI-ARGUMENT)",
            subtitle = "Strategik xavflar va resurslar tahlili",
            icon = Icons.Default.Hub,
            accentColor = JarvisAmberGold,
            content = debate.strategyCounter
        )

        // 4. Round 3: AEGIS-GUARD Security Challenge
        AgentReportCard(
            codeName = "3-RAUND: AEGIS-GUARD (XAVFSIZLIK SHARTI)",
            subtitle = "Zaif nuqtalar va kiber-himoya talabi",
            icon = Icons.Default.Security,
            accentColor = JarvisEmerald,
            content = debate.securityAudit
        )

        // 5. Round 4: VISION-NOVA Breakthrough Synthesis
        AgentReportCard(
            codeName = "4-RAUND: VISION-NOVA (INNOVATSION SINTEZ)",
            subtitle = "Qarama-qarshi fikrlarni birlashtiruvchi kelajak yechimi",
            icon = Icons.Default.Lightbulb,
            accentColor = JarvisViolet,
            content = debate.creativeBreakthrough
        )
    }
}

@Composable
private fun StreamingAgentCard(
    agent: AiAgentRole,
    prompt: String,
    text: String,
    isStreaming: Boolean,
    onSpeak: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, agent.accentColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${agent.codeName} // ${agent.titleUz}",
                        style = MaterialTheme.typography.labelLarge,
                        color = agent.accentColor
                    )
                    Text(
                        text = if (isStreaming) "JONLI OQIM (STREAMING)..." else "TAHLIL YAKUNLANDI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isStreaming) {
                    IconButton(onClick = onSpeak) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak response",
                            tint = agent.accentColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Buyruq: $prompt",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MultiAiCouncilResultSection(
    response: MultiAiCouncilResponse,
    onSpeakSummary: () -> Unit,
    onSpeakFull: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag("council_result_section")
    ) {
        // If a hardware/system action was executed, show a dedicated confirmation badge
        if (!response.executedActionStatus.isNullOrBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisEmerald.copy(alpha = 0.16f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, JarvisEmerald)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = JarvisEmerald
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TELEFON AMALI BAJARILDI (${response.deviceAction})",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisEmerald
                        )
                        Text(
                            text = response.executedActionStatus,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 1. JARVIS-CORE Voice Executive Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, JarvisCyan)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = JarvisCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "JARVIS-CORE // OVOZLI XULOSA",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisCyan
                            )
                            Text(
                                text = "Niyat: ${response.detectedIntent} • Ishonch: ${response.confidenceScore}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row {
                        OutlinedButton(
                            onClick = onSpeakSummary,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, JarvisCyan),
                            modifier = Modifier.testTag("speak_summary_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("O'qish", style = MaterialTheme.typography.labelSmall, color = JarvisCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = response.voiceSpokenSummary,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onSpeakFull,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, JarvisElectricBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = JarvisElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Barcha Agentlar Hisobotini Ovozda Eshitish",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisElectricBlue
                    )
                }
            }
        }

        // 2. NEXUS-CODE (Technical Architect AI)
        AgentReportCard(
            codeName = "NEXUS-CODE // TEXNIK ARXITEKTOR AI",
            subtitle = "Tizim arxitekturasi, modellar zanjiri va dasturiy yechim",
            icon = Icons.Default.Code,
            accentColor = JarvisElectricBlue,
            content = response.architectReport
        )

        // 3. STRAT-MIND (Strategist AI Plan)
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, JarvisAmberGold.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = JarvisAmberGold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "STRAT-MIND // STRATEGIYA VA BOSQICHLAR",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisAmberGold
                        )
                        Text(
                            text = "Qadamma-qadam amalga oshirish xaritasi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                response.strategistPlan.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(JarvisAmberGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisAmberGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 4. AEGIS-GUARD (Security & Hardware AI)
        AgentReportCard(
            codeName = "AEGIS-GUARD // XAVFSIZLIK VA OFLAYN HIMOYA",
            subtitle = "Biometrik ovoz himoyasi, maxfiylik va zaxira protokollari",
            icon = Icons.Default.Security,
            accentColor = JarvisEmerald,
            content = response.sentinelSecurityNote
        )

        // 5. VISION-NOVA (Creative & Futuristic UX AI)
        AgentReportCard(
            codeName = "VISION-NOVA // KELAJAK INNOVATSIYASI",
            subtitle = "Golografik UX, multimodal ko'rish va super-imkoniyat g'oyasi",
            icon = Icons.Default.Lightbulb,
            accentColor = JarvisViolet,
            content = response.creativeSpark
        )
    }
}

@Composable
private fun AgentReportCard(
    codeName: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    content: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = codeName,
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
