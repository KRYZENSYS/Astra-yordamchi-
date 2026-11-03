package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.network.AiStudioApiNode
import com.example.network.ApiNodePowerState
import com.example.network.ApiProviderType
import com.example.network.ApiTaskScope
import com.example.network.ClusterTokenSummary
import com.example.ui.theme.JarvisAmberGold
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDeepNavy
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisViolet
import java.util.Locale

private fun formatTokensUi(tokens: Long): String {
    return when {
        tokens >= 1_000_000L -> String.format(Locale.US, "%.2fM", tokens / 1_000_000.0)
        tokens >= 10_000L -> String.format(Locale.US, "%.1fK", tokens / 1_000.0)
        else -> tokens.toString()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiStudioClusterScreen(
    apiNodes: List<AiStudioApiNode>,
    clusterStatusBanner: String,
    tokenSummary: ClusterTokenSummary,
    autoSleepIdleEnabled: Boolean,
    autoSleepIdleMinutes: Int,
    ecoSingleAwakeMode: Boolean,
    autoFailoverEnabled: Boolean,
    roundRobinKeysEnabled: Boolean,
    isEmbeddedServerRunning: Boolean,
    embeddedServerAddress: String,
    embeddedServerRequestsCount: Int,
    lastEmbeddedServerEvent: String,
    localServerBridgeEnabled: Boolean,
    localServerEndpointUrl: String,
    pingingNodeId: String?,
    onAddApiNode: (label: String, apiKey: String, modelName: String, scope: ApiTaskScope, startSleeping: Boolean, dailyTokenLimit: Long, serverUrl: String) -> Unit,
    onAddMultipleApiKeysBatch: (rawKeysInput: String, modelName: String, scope: ApiTaskScope, startSleeping: Boolean, dailyTokenLimit: Long, serverUrl: String) -> Unit,
    onUpdateNodeApiKeyAndLimit: (nodeId: String, newApiKey: String, newDailyLimit: Long, newServerUrl: String?) -> Unit,
    onToggleEmbeddedLocalServer: (enable: Boolean) -> Unit,
    onConfigureLocalServerBridge: (enabled: Boolean, endpointUrl: String) -> Unit,
    onResetTokenCounters: (nodeId: String?) -> Unit,
    onSpeakTokenReport: () -> Unit,
    onToggleNodeSleep: (nodeId: String) -> Unit,
    onSetPrimaryNode: (nodeId: String) -> Unit,
    onPingNode: (nodeId: String) -> Unit,
    onRemoveNode: (nodeId: String) -> Unit,
    onSleepAllStandby: () -> Unit,
    onWakeAllApis: () -> Unit,
    onRotateActiveApi: () -> Unit,
    onToggleAutoSleepIdle: (enabled: Boolean, minutes: Int) -> Unit,
    onToggleEcoSingleAwake: (enabled: Boolean) -> Unit,
    onToggleAutoFailover: (enabled: Boolean) -> Unit,
    onToggleRoundRobinKeys: (enabled: Boolean) -> Unit,
    onSpeakText: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    var newNodeLabel by rememberSaveable { mutableStateOf("") }
    var newNodeApiKey by rememberSaveable { mutableStateOf("") }
    var newNodeServerUrl by rememberSaveable { mutableStateOf("") }
    var batchApiKeysText by rememberSaveable { mutableStateOf("") }
    var batchServerUrlText by rememberSaveable { mutableStateOf("") }
    var bridgeUrlInput by rememberSaveable(localServerEndpointUrl) { mutableStateOf(localServerEndpointUrl) }
    var showApiKeyText by remember { mutableStateOf(false) }
    var selectedModel by rememberSaveable { mutableStateOf("gemini-3.8-flash") }
    var selectedScope by rememberSaveable { mutableStateOf(ApiTaskScope.UNIVERSAL) }
    var selectedTokenLimit by rememberSaveable { mutableLongStateOf(1_000_000L) }
    var startInSleepMode by rememberSaveable { mutableStateOf(false) }
    var showAddForm by rememberSaveable { mutableStateOf(false) }
    var showBatchForm by rememberSaveable { mutableStateOf(false) }
    var editingNodeId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingNodeKeyText by rememberSaveable { mutableStateOf("") }
    var editingNodeServerText by rememberSaveable { mutableStateOf("") }

    val availableModels = remember {
        listOf(
            "gemini-3.8-flash",
            "gemini-3.1-pro-preview",
            "gemini-2.5-flash",
            "gemini-2.5-pro"
        )
    }

    val tokenLimitPresets = remember {
        listOf(
            100_000L to "100K tok",
            500_000L to "500K tok",
            1_000_000L to "1M tok",
            5_000_000L to "5M tok"
        )
    }

    val awakeCount = apiNodes.count { it.powerState == ApiNodePowerState.ACTIVE }
    val sleepingCount = apiNodes.count { it.powerState != ApiNodePowerState.ACTIVE }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_studio_cluster_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Cluster Hero Header & Sleep/Wake Master Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = BorderStroke(1.5.dp, JarvisCyan.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    JarvisDeepNavy,
                                    JarvisObsidian
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(JarvisCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = JarvisCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AQ. API KEY & LOKAL SERVER KLASTERI",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = JarvisCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "AQ. kalitlar, Internet/Lokal Server va Token hisoblagich",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = JarvisTextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { onSpeakText(clusterStatusBanner) }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Holatni ovozda eshitish",
                                tint = JarvisCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Awake vs Sleeping Status Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = JarvisEmerald.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, JarvisEmerald.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = JarvisEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "$awakeCount ta UYG'OQ",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = JarvisEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Faol AQ./API Key",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JarvisTextSecondary
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = JarvisViolet.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, JarvisViolet.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = JarvisViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "$sleepingCount ta UYQUDA",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = JarvisViolet,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Zaxira (Standby)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JarvisTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = clusterStatusBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisAmberGold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Master Sleep / Wake / Rotate Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSleepAllStandby,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisViolet,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sleep_standby_apis")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zaxirani Uxlatish",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Button(
                            onClick = onWakeAllApis,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisEmerald,
                                contentColor = JarvisObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_wake_all_apis")
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hammasini Uyg'otish",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRotateActiveApi,
                            border = BorderStroke(1.dp, JarvisCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_rotate_api_node")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Keyingi AQ. Key",
                                style = MaterialTheme.typography.labelMedium,
                                color = JarvisCyan
                            )
                        }

                        Button(
                            onClick = {
                                showAddForm = !showAddForm
                                if (showAddForm) showBatchForm = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisAmberGold,
                                contentColor = JarvisObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_toggle_add_api_form")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLink,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showAddForm) "Formani Yopish" else "+ AQ. Key Ulash",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            showBatchForm = !showBatchForm
                            if (showBatchForm) showAddForm = false
                        },
                        border = BorderStroke(1.dp, JarvisAmberGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_toggle_batch_keys_form")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryAdd,
                            contentDescription = null,
                            tint = JarvisAmberGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showBatchForm) {
                                "Ommaviy AQ. Key Formasini Yopish"
                            } else {
                                "Bir Nechta AQ. API Key'ni Birdaniga Ulash (Batch)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = JarvisAmberGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Local & Internet HTTP Server Gateway Card (Port 8080 + Remote Bridge)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("local_server_gateway_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = BorderStroke(
                    1.2.dp,
                    if (isEmbeddedServerRunning) JarvisEmerald.copy(alpha = 0.7f) else JarvisCyan.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isEmbeddedServerRunning) {
                                            JarvisEmerald.copy(alpha = 0.16f)
                                        } else {
                                            JarvisCyan.copy(alpha = 0.16f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = if (isEmbeddedServerRunning) JarvisEmerald else JarvisCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "LOKAL & INTERNET SERVER SHLYUZI",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isEmbeddedServerRunning) JarvisEmerald else JarvisCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isEmbeddedServerRunning) {
                                        "Faol manzil: $embeddedServerAddress • So'rovlar: $embeddedServerRequestsCount ta"
                                    } else {
                                        "Lokal HTTP Server o'chirilgan"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = JarvisTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isEmbeddedServerRunning,
                            onCheckedChange = onToggleEmbeddedLocalServer,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisObsidian,
                                checkedTrackColor = JarvisEmerald
                            ),
                            modifier = Modifier.testTag("switch_embedded_local_server")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = JarvisDeepNavy,
                        border = BorderStroke(1.dp, JarvisBorder.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Ichki Server Endpointlari (Internet / Wi-Fi / LAN orqali):",
                                style = MaterialTheme.typography.labelMedium,
                                color = JarvisCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Web Boshqaruv HUD: $embeddedServerAddress/\n" +
                                    "• AI Proxy (AQ. Key bilan): POST $embeddedServerAddress/api/generate\n" +
                                    "• Masofaviy Buyruq: GET $embeddedServerAddress/api/command?q=Fonarikni+yoq\n" +
                                    "• Token va Holat JSON: GET $embeddedServerAddress/api/status",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Oxirgi hodisa: $lastEmbeddedServerEvent",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisAmberGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // External / Local Server Bridge Toggle & URL Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tashqi / Lokal Server Orqali Ishlash (Internet Bridge)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "AQ. API kalitlarini siz ko'rsatgan lokal yoki internet serverga yo'naltiradi",
                                style = MaterialTheme.typography.bodySmall,
                                color = JarvisTextSecondary
                            )
                        }
                        Switch(
                            checked = localServerBridgeEnabled,
                            onCheckedChange = { enabled ->
                                onConfigureLocalServerBridge(enabled, bridgeUrlInput)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisObsidian,
                                checkedTrackColor = JarvisCyan
                            ),
                            modifier = Modifier.testTag("switch_local_server_bridge")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = bridgeUrlInput,
                            onValueChange = { bridgeUrlInput = it },
                            label = { Text("Server URL (http://127.0.0.1:8080 yoki https://...)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_local_server_url")
                        )

                        Button(
                            onClick = {
                                onConfigureLocalServerBridge(true, bridgeUrlInput)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = JarvisObsidian
                            ),
                            modifier = Modifier.testTag("btn_save_local_server_url")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lan,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ulash", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2B. Termux Orqali APK Yig'ish (Repository Scripts & 1-Tap Copy)
        item {
            val clipboardManager = LocalClipboardManager.current
            var termuxCopiedBanner by remember { mutableStateOf(false) }
            val termuxQuickCommand = "pkg update -y && pkg install git openjdk-21 gradle aapt2 -y && chmod +x termux-setup-sdk.sh termux-build-apk.sh && ./termux-setup-sdk.sh && ./termux-build-apk.sh"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("termux_apk_builder_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = BorderStroke(1.dp, JarvisViolet.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = JarvisViolet
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TERMUX ORQALI APK YIG'ISH (REPO SKRIPTLARI)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = JarvisViolet,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "termux-setup-sdk.sh • termux-build-apk.sh • ARM64 AAPT2",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisTextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onSpeakText(
                                    "Repozitoriyga Termux orqali APK yig'ish skriptlari qo'shildi. " +
                                        "Termux ichida termux-setup-sdk.sh va termux-build-apk.sh skriptlarini ishga tushirsangiz, " +
                                        "tayyor APK fayli avtomatik ravishda Download papkasiga Astrafolio-Jarvis.apk nomi bilan saqlanadi."
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Termux qo'llanmasini eshitish",
                                tint = JarvisViolet
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Repozitoriy ildizida `/termux-setup-sdk.sh`, `/termux-build-apk.sh` va `/TERMUX_APK_BUILD_UZ.md` fayllari tayyor. Telefonning o'zida kompyutersiz APK chiqarish uchun quyidagi buyruqni Termux'ga nusxalang:",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = JarvisObsidian,
                        border = BorderStroke(1.dp, JarvisViolet.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = termuxQuickCommand,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyan,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(termuxQuickCommand))
                                termuxCopiedBanner = true
                                onSpeakText("Termux APK yig'ish buyrug'i nusxalandi.")
                            },
                            border = BorderStroke(1.dp, JarvisViolet),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_copy_termux_script")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = JarvisViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (termuxCopiedBanner) "Nusxalandi ✓" else "Termux Buyrug'ini Nusxalash",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisViolet
                            )
                        }
                    }
                }
            }
        }

        // 3. Real-Time Token Usage Counter & Analytics Dashboard Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("token_analytics_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = BorderStroke(1.dp, JarvisAmberGold.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = JarvisAmberGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "REAL-VAQT TOKEN HISOBLAGICHI",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = JarvisAmberGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Barcha AQ. va AI Studio API kalitlar bo'yicha sarflangan tokenlar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = JarvisTextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = JarvisCyan.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Oxirgi: +${tokenSummary.lastRequestTokens} tok",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Metric Boxes: Total Tokens, Prompt Tokens, Output Tokens
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = JarvisDeepNavy,
                            border = BorderStroke(1.dp, JarvisAmberGold.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = formatTokensUi(tokenSummary.totalTokens),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = JarvisAmberGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.testTag("total_tokens_value")
                                )
                                Text(
                                    text = "Jami Token",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisTextSecondary
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = JarvisDeepNavy,
                            border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = formatTokensUi(tokenSummary.totalPromptTokens),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = JarvisCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Kiruvchi (Prompt)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisTextSecondary
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = JarvisDeepNavy,
                            border = BorderStroke(1.dp, JarvisEmerald.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = formatTokensUi(tokenSummary.totalOutputTokens),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = JarvisEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Chiquvchi (Javob)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ulangan AQ./Shaxsiy Key'lar: ${tokenSummary.connectedKeysCount} ta • So'rovlar: ${tokenSummary.totalRequestsCount} ta",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisTextSecondary
                        )
                        Text(
                            text = "Faol: ${tokenSummary.lastUsedNodeLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSpeakTokenReport,
                            border = BorderStroke(1.dp, JarvisCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_speak_token_report")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ovozli Hisobot",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisCyan
                            )
                        }

                        OutlinedButton(
                            onClick = { onResetTokenCounters(null) },
                            border = BorderStroke(1.dp, JarvisCrimson.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_reset_all_tokens")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = JarvisCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tokenlarni 0 Qilish",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisCrimson
                            )
                        }
                    }
                }
            }
        }

        // 4A. Connect Single AQ. / AI Studio API Key Form (Expandable)
        if (showAddForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                    border = BorderStroke(1.dp, JarvisAmberGold.copy(alpha = 0.65f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = JarvisAmberGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "YANGI AQ. API KEY YOKI LOKAL SERVER ULASH",
                                style = MaterialTheme.typography.titleSmall,
                                color = JarvisAmberGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "AQ. bilan boshlanuvchi AI Studio API kalitingizni (AQ....) va ixtiyoriy Lokal/Internet server manzilini kiriting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisTextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newNodeLabel,
                            onValueChange = { newNodeLabel = it },
                            label = { Text("API Kalit Nomi (masalan: AI Studio AQ. Key #4)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisAmberGold,
                                unfocusedBorderColor = JarvisBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_api_label")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newNodeApiKey,
                            onValueChange = { newNodeApiKey = it },
                            label = { Text("AI Studio API Key (AQ.... / AIzaSy...)") },
                            placeholder = { Text("AQ.Ab8RN6...") },
                            singleLine = true,
                            visualTransformation = if (showApiKeyText) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                FilterChip(
                                    selected = showApiKeyText,
                                    onClick = { showApiKeyText = !showApiKeyText },
                                    label = { Text(if (showApiKeyText) "Yashirish" else "Ko'rsatish") },
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_api_key")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newNodeServerUrl,
                            onValueChange = { newNodeServerUrl = it },
                            label = { Text("Lokal / Internet Server URL (ixtiyoriy: http://127.0.0.1:8080)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisEmerald,
                                unfocusedBorderColor = JarvisBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_api_server_url")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Kunlik Token Kvota Limiti (Tugaganda keyingi API'ga o'tadi):",
                            style = MaterialTheme.typography.labelMedium,
                            color = JarvisEmerald
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tokenLimitPresets.forEach { (limitVal, labelText) ->
                                FilterChip(
                                    selected = selectedTokenLimit == limitVal,
                                    onClick = { selectedTokenLimit = limitVal },
                                    label = { Text(labelText) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = JarvisEmerald.copy(alpha = 0.22f),
                                        selectedLabelColor = JarvisEmerald
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "AI Studio Modelini Tanlang:",
                            style = MaterialTheme.typography.labelMedium,
                            color = JarvisCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableModels.forEach { model ->
                                FilterChip(
                                    selected = selectedModel == model,
                                    onClick = { selectedModel = model },
                                    label = { Text(model) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = JarvisCyan.copy(alpha = 0.22f),
                                        selectedLabelColor = JarvisCyan
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Ushbu API Qaysi Bo'limga Javob Beradi:",
                            style = MaterialTheme.typography.labelMedium,
                            color = JarvisAmberGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ApiTaskScope.entries.forEach { scope ->
                                FilterChip(
                                    selected = selectedScope == scope,
                                    onClick = { selectedScope = scope },
                                    label = { Text(scope.labelUz) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = JarvisAmberGold.copy(alpha = 0.22f),
                                        selectedLabelColor = JarvisAmberGold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dastlab Uyqu (Standby) Rejimida Qo'shish",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Faqat asosiy API band bo'lganda yoki kvotasi to'lganda uyg'onadi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = JarvisTextSecondary
                                )
                            }
                            Switch(
                                checked = startInSleepMode,
                                onCheckedChange = { startInSleepMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = JarvisObsidian,
                                    checkedTrackColor = JarvisViolet
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onAddApiNode(
                                    newNodeLabel,
                                    newNodeApiKey,
                                    selectedModel,
                                    selectedScope,
                                    startInSleepMode,
                                    selectedTokenLimit,
                                    newNodeServerUrl
                                )
                                newNodeLabel = ""
                                newNodeApiKey = ""
                                newNodeServerUrl = ""
                                showAddForm = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = JarvisObsidian
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_save_new_api_node")
                        ) {
                            Icon(imageVector = Icons.Default.AddLink, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AQ. API Kalitni Klasterga Ulash",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4B. Batch Multi-Key Paste Form (Connect Several AQ. API Keys at Once)
        if (showBatchForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                    border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LibraryAdd,
                                contentDescription = null,
                                tint = JarvisCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BIR NECHTA AQ. API KEY'NI BIRDANIGA ULASH",
                                style = MaterialTheme.typography.titleSmall,
                                color = JarvisCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Har bir qatorga yoki vergul orqali bir nechta AQ. bilan boshlanuvchi AI Studio API kalitlarini (masalan: AQ.Ab8RN6...) tashlang. Tizim ularni avtomatik ulab, har birining tokenini alohida hisoblaydi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisTextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = batchApiKeysText,
                            onValueChange = { batchApiKeysText = it },
                            label = { Text("Bir nechta AQ. API Key (har bir qatorda 1 tadan AQ....)") },
                            placeholder = { Text("AQ.KeyOne12345\nAQ.KeyTwo67890") },
                            minLines = 3,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_batch_api_keys")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = batchServerUrlText,
                            onValueChange = { batchServerUrlText = it },
                            label = { Text("Lokal / Internet Server URL (ixtiyoriy: http://127.0.0.1:8080)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisEmerald,
                                unfocusedBorderColor = JarvisBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (batchApiKeysText.isNotBlank()) {
                                    onAddMultipleApiKeysBatch(
                                        batchApiKeysText,
                                        selectedModel,
                                        selectedScope,
                                        startInSleepMode,
                                        selectedTokenLimit,
                                        batchServerUrlText
                                    )
                                    batchApiKeysText = ""
                                    batchServerUrlText = ""
                                    showBatchForm = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisAmberGold,
                                contentColor = JarvisObsidian
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_save_batch_api_keys")
                        ) {
                            Icon(imageVector = Icons.Default.LibraryAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Barcha Kiritilgan AQ. API Key'larni Ulash",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. Smart Auto-Sleep, Load-Balancing & Failover Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = BorderStroke(1.dp, JarvisViolet.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = JarvisViolet
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AVTO-UYQU, NAVBAT VA FAILOVER REJIMLARI",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisViolet,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle 0: Round-Robin Multi-Key Load Balancing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AQ. API Key'larni Navbat Bilan Ishlatish (Round-Robin)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Har bir so'rovda token yuklamasini barcha uyg'oq API kalitlar o'rtasida teng taqsimlaydi",
                                style = MaterialTheme.typography.bodySmall,
                                color = JarvisTextSecondary
                            )
                        }
                        Switch(
                            checked = roundRobinKeysEnabled,
                            onCheckedChange = onToggleRoundRobinKeys,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisObsidian,
                                checkedTrackColor = JarvisAmberGold
                            ),
                            modifier = Modifier.testTag("switch_round_robin_keys")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle 1: Auto-Sleep Idle Timer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Harakatsizlikda Avto-Uyqu ($autoSleepIdleMinutes daqiqa)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Ishlatilmayotgan qo'shimcha AI Studio API'lar avtomatik uxlaydi",
                                style = MaterialTheme.typography.bodySmall,
                                color = JarvisTextSecondary
                            )
                        }
                        Switch(
                            checked = autoSleepIdleEnabled,
                            onCheckedChange = { onToggleAutoSleepIdle(it, autoSleepIdleMinutes) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisObsidian,
                                checkedTrackColor = JarvisViolet
                            ),
                            modifier = Modifier.testTag("switch_auto_sleep_idle")
                        )
                    }

                    if (autoSleepIdleEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 5, 10, 15).forEach { mins ->
                                FilterChip(
                                    selected = autoSleepIdleMinutes == mins,
                                    onClick = { onToggleAutoSleepIdle(true, mins) },
                                    label = { Text("$mins daq") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = JarvisViolet.copy(alpha = 0.25f),
                                        selectedLabelColor = JarvisViolet
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle 2: Eco Single-Awake Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Yagona Uyg'oq API Rejimi (Eco-Sleep)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Faqat 1 ta asosiy API uyg'oq turadi, qolganlari navbat kelguncha uxlaydi",
                                style = MaterialTheme.typography.bodySmall,
                                color = JarvisTextSecondary
                            )
                        }
                        Switch(
                            checked = ecoSingleAwakeMode,
                            onCheckedChange = onToggleEcoSingleAwake,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisObsidian,
                                checkedTrackColor = JarvisEmerald
                            ),
                            modifier = Modifier.testTag("switch_eco_single_awake")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle 3: Auto-Wake Failover
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Avto-Uyg'onish (Kvota yoki Token Limiti To'lganda)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "API kalitning token limiti to'lsa, uxlayotgan zaxira API darhol uyg'onib javob beradi",
                                style = MaterialTheme.typography.bodySmall,
                                color = JarvisTextSecondary
                            )
                        }
                        Switch(
                            checked = autoFailoverEnabled,
                            onCheckedChange = onToggleAutoFailover,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisObsidian,
                                checkedTrackColor = JarvisCyan
                            ),
                            modifier = Modifier.testTag("switch_auto_failover")
                        )
                    }
                }
            }
        }

        // 6. Section Header for Connected Nodes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ULANGAN AQ. KALITLAR VA TOKENLAR (${apiNodes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = JarvisCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ovoz: «Nechta token ishlatildi?»",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSecondary
                )
            }
        }

        // 7. Individual AI Studio API Node Cards with Per-Key Token Progress & Key/Server Editor
        items(apiNodes, key = { it.id }) { node ->
            val isAwake = node.powerState == ApiNodePowerState.ACTIVE
            val stateColor = when (node.powerState) {
                ApiNodePowerState.ACTIVE -> JarvisEmerald
                ApiNodePowerState.SLEEPING -> JarvisViolet
                ApiNodePowerState.COOLDOWN -> JarvisAmberGold
            }
            val isEditingThisNode = editingNodeId == node.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_node_card_${node.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = BorderStroke(
                    width = if (node.isPrimary) 1.5.dp else 1.dp,
                    color = if (node.isPrimary) JarvisCyan else stateColor.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(stateColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAwake) Icons.Default.Bolt else Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = stateColor
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = node.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (node.isPrimary) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = JarvisCyan.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "ASOSIY",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = JarvisCyan,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${node.modelName} • ${node.taskScope.labelUz}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = JarvisTextSecondary
                                )
                            }
                        }

                        // Sleep / Wake Switch for this specific API node
                        Column(horizontalAlignment = Alignment.End) {
                            Switch(
                                checked = isAwake,
                                onCheckedChange = { onToggleNodeSleep(node.id) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = JarvisObsidian,
                                    checkedTrackColor = JarvisEmerald,
                                    uncheckedThumbColor = JarvisViolet,
                                    uncheckedTrackColor = JarvisDeepNavy
                                ),
                                modifier = Modifier.testTag("toggle_node_sleep_${node.id}")
                            )
                            Text(
                                text = node.powerState.labelUz,
                                style = MaterialTheme.typography.labelSmall,
                                color = stateColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Per-Key Token Breakdown Badges
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = JarvisDeepNavy,
                        border = BorderStroke(1.dp, JarvisBorder.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sarflangan Token: ${formatTokensUi(node.totalTokens)} / ${formatTokensUi(node.dailyTokenLimit)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = JarvisAmberGold,
                                    fontWeight = FontWeight.Bold
                                )
                                if (node.lastRequestTokens > 0) {
                                    Text(
                                        text = "Oxirgi: +${node.lastRequestTokens} tok",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JarvisCyan
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { node.tokenUsageFraction() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (node.tokenUsageFraction() > 0.85f) JarvisCrimson else JarvisAmberGold,
                                trackColor = JarvisSurfaceCard
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Kiruvchi: ${node.promptTokens} tok",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisCyan
                                )
                                Text(
                                    text = "Javob: ${node.outputTokens} tok",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisEmerald
                                )
                                Text(
                                    text = "So'rov: ${node.totalRequests} ta",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Telemetry chips: Provider, Masked Key, Server URL, Latency
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = JarvisDeepNavy,
                            border = BorderStroke(1.dp, JarvisBorder.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (node.providerType) {
                                        ApiProviderType.FIREBASE_AI_LOGIC -> Icons.Default.CloudQueue
                                        ApiProviderType.LOCAL_SERVER_GATEWAY -> Icons.Default.Dns
                                        ApiProviderType.CUSTOM_AI_STUDIO_KEY -> Icons.Default.Shield
                                    },
                                    contentDescription = null,
                                    tint = JarvisCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${node.providerType.badgeUz}: ${node.maskedApiKey()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisCyan
                                )
                            }
                        }

                        if (node.serverUrl.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = JarvisDeepNavy
                            ) {
                                Text(
                                    text = "Server: ${node.serverUrl}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisAmberGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (node.lastLatencyMs > 0L) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = JarvisDeepNavy
                            ) {
                                Text(
                                    text = "Tezlik: ${node.lastLatencyMs} ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = node.lastStatusNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )

                    // Inline AQ. API Key & Server URL Edit Box
                    if (isEditingThisNode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = editingNodeKeyText,
                            onValueChange = { editingNodeKeyText = it },
                            label = { Text("Yangi AQ. API Key (AQ.... / AIzaSy...)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisAmberGold,
                                unfocusedBorderColor = JarvisBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_edit_node_key_${node.id}")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editingNodeServerText,
                            onValueChange = { editingNodeServerText = it },
                            label = { Text("Lokal / Internet Server URL (ixtiyoriy: http://127.0.0.1:8080)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onUpdateNodeApiKeyAndLimit(
                                        node.id,
                                        editingNodeKeyText,
                                        node.dailyTokenLimit,
                                        editingNodeServerText
                                    )
                                    editingNodeId = null
                                    editingNodeKeyText = ""
                                    editingNodeServerText = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JarvisAmberGold,
                                    contentColor = JarvisObsidian
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Kalit va Serverni Saqlash", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    editingNodeId = null
                                    editingNodeKeyText = ""
                                    editingNodeServerText = ""
                                }
                            ) {
                                Text("Bekor")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Node Action Row: Wake/Sleep, Ping Test, Edit Key, Reset Node Tokens, Primary, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onToggleNodeSleep(node.id) },
                            border = BorderStroke(1.dp, stateColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isAwake) Icons.Default.Bedtime else Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = stateColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAwake) "Uxlatish" else "Uyg'otish",
                                style = MaterialTheme.typography.labelSmall,
                                color = stateColor
                            )
                        }

                        OutlinedButton(
                            onClick = { onPingNode(node.id) },
                            enabled = pingingNodeId != node.id,
                            border = BorderStroke(1.dp, JarvisCyan),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_ping_${node.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (pingingNodeId == node.id) "Sinov..." else "Ping",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisCyan
                            )
                        }

                        IconButton(
                            onClick = {
                                if (editingNodeId == node.id) {
                                    editingNodeId = null
                                } else {
                                    editingNodeId = node.id
                                    editingNodeKeyText = node.apiKey
                                    editingNodeServerText = node.serverUrl
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("btn_edit_key_${node.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "AQ. API Kalit va Serverni Tahrirlash",
                                tint = JarvisAmberGold
                            )
                        }

                        IconButton(
                            onClick = { onResetTokenCounters(node.id) },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("btn_reset_tokens_${node.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Tokenni 0 Qilish",
                                tint = JarvisCyan
                            )
                        }

                        if (!node.isPrimary) {
                            IconButton(
                                onClick = { onSetPrimaryNode(node.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Asosiy API qilish",
                                    tint = JarvisAmberGold
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Asosiy API",
                                tint = JarvisCyan,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(18.dp)
                            )
                        }

                        if (apiNodes.size > 1) {
                            IconButton(
                                onClick = { onRemoveNode(node.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "API tugunini o'chirish",
                                    tint = JarvisCrimson
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
