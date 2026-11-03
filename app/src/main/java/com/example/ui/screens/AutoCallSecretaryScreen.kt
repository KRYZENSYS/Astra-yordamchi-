package com.example.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.cloud.AutoCallReplyLog
import com.example.data.cloud.VoiceNoteItem
import com.example.ui.theme.JarvisAmberGold
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisViolet
import com.example.viewmodel.AutoCallSecretaryUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val STATUS_PRESETS = listOf(
    "Uchrashuvdaman",
    "Mashina haydayapman",
    "Kod yozyapman",
    "Dam olyapman",
    "Maxsus AI Shablon"
)

private val QUICK_CALLER_SAMPLES = listOf(
    "+998 90 123 45 67" to "Bosh Direktor (Muhim)",
    "+998 93 555 77 88" to "Mijoz (Loyiha bo'yicha)",
    "+998 97 700 11 22" to "Yaqin Do'st"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutoCallSecretaryScreen(
    isBackgroundRunning: Boolean,
    autoCallEnabled: Boolean,
    currentStatusPreset: String,
    customReplyTemplate: String,
    screenedCallsCount: Int,
    lastScreenedCallInfo: String?,
    secretaryUiState: AutoCallSecretaryUiState,
    cloudCallLogs: List<AutoCallReplyLog>,
    cloudVoiceNotes: List<VoiceNoteItem>,
    onToggleBackgroundService: (Boolean) -> Unit,
    onToggleAutoCallResponder: (Boolean) -> Unit,
    onSelectStatusPreset: (String) -> Unit,
    onUpdateCustomTemplate: (String) -> Unit,
    onGenerateAndRespondToCall: (String, String) -> Unit,
    onSendSmsReply: (String, String) -> Unit,
    onDeleteCallLog: (String) -> Unit,
    onCreateVoiceNote: (String, String) -> Unit,
    onDeleteVoiceNote: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)
    val context = LocalContext.current

    var callerNumberInput by rememberSaveable { mutableStateOf("+998 90 123 45 67") }
    var callerLabelInput by rememberSaveable { mutableStateOf("Bosh Direktor (Muhim)") }
    var noteTitleInput by rememberSaveable { mutableStateOf("") }
    var noteContentInput by rememberSaveable { mutableStateOf("") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onToggleBackgroundService(true)
    }

    val callScreeningRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("auto_call_secretary_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 24/7 Background Sentinel Control Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (isBackgroundRunning) JarvisEmerald else JarvisCyan.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(
                                        if (isBackgroundRunning) JarvisEmerald.copy(alpha = 0.2f)
                                        else JarvisCyan.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (isBackgroundRunning) JarvisEmerald else JarvisCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "24/7 ORQA FON TIZIMI (BACKGROUND OS)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isBackgroundRunning) JarvisEmerald else JarvisCyan
                                )
                                Text(
                                    text = if (isBackgroundRunning) {
                                        "Faol • Ilova yopilganda ham bildirishnoma va avto-kotib ishlaydi"
                                    } else {
                                        "O'chiq • Doimiy orqa fonda ishlash uchun yoqing"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (!isBackgroundRunning) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onToggleBackgroundService(true)
                                }
                            } else {
                                onToggleBackgroundService(false)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBackgroundRunning) JarvisCrimson else JarvisEmerald,
                            contentColor = JarvisObsidian
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_background_service_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBackgroundRunning) {
                                "24/7 Orqa Fon Rejimini To'xtatish"
                            } else {
                                "24/7 Orqa Fon Rejimini Yoqish (Doimiy Faol)"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. AI Auto-Call Responder Secretary Configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, JarvisAmberGold.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PhoneCallback,
                                contentDescription = null,
                                tint = JarvisAmberGold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI QO'NG'IROQ AVTO-JAVOBGAR (KOTIB)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = JarvisAmberGold
                                )
                                Text(
                                    text = "Sizga telefon qilganlarga AI o'zbek tilida avtomatik javob beradi • Javoblar: $screenedCallsCount ta",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = autoCallEnabled,
                            onCheckedChange = onToggleAutoCallResponder,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisObsidian,
                                checkedTrackColor = JarvisAmberGold
                            ),
                            modifier = Modifier.testTag("auto_call_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            requestCallScreeningRole(context) { intent ->
                                callScreeningRoleLauncher.launch(intent)
                            }
                        },
                        border = BorderStroke(1.dp, JarvisCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tizimda Asosiy Qo'ng'iroq Kotibi Sifatida Biriktirish",
                            color = JarvisCyan,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Hozirgi bandlik holatingizni tanlang:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        STATUS_PRESETS.forEach { preset ->
                            FilterChip(
                                selected = currentStatusPreset == preset,
                                onClick = { onSelectStatusPreset(preset) },
                                label = { Text(preset) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JarvisAmberGold.copy(alpha = 0.22f),
                                    selectedLabelColor = JarvisAmberGold
                                )
                            )
                        }
                    }

                    if (currentStatusPreset == "Maxsus AI Shablon") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customReplyTemplate,
                            onValueChange = onUpdateCustomTemplate,
                            label = { Text("Maxsus o'zbekcha javob matni") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisAmberGold,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    if (!lastScreenedCallInfo.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = JarvisEmerald.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, JarvisEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Oxirgi avto-javob: $lastScreenedCallInfo",
                                style = MaterialTheme.typography.bodySmall,
                                color = JarvisEmerald,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Incoming Call AI Responder & Simulator Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = JarvisCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KIRUVCHI QO'NG'IROQQA AVTOMATIK AI JAVOB BERISH",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Kimdir qo'ng'iroq qilganda Gemini 3.8 Flash sizning holatingizga mos o'zbekcha javob tuzadi, ovozda o'qib eshittiradi va Firebase bulutiga saqlaydi:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QUICK_CALLER_SAMPLES.forEach { (num, label) ->
                            FilterChip(
                                selected = callerNumberInput == num,
                                onClick = {
                                    callerNumberInput = num
                                    callerLabelInput = label
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JarvisCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = JarvisCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = callerNumberInput,
                        onValueChange = { callerNumberInput = it },
                        label = { Text("Qo'ng'iroq qiluvchi telefon raqami") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("caller_number_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = callerLabelInput,
                        onValueChange = { callerLabelInput = it },
                        label = { Text("Kim qo'ng'iroq qilmoqda (Ism yoki izoh)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("caller_label_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onGenerateAndRespondToCall(callerNumberInput, callerLabelInput)
                        },
                        enabled = secretaryUiState !is AutoCallSecretaryUiState.GeneratingReply,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = JarvisObsidian
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trigger_auto_call_reply_button")
                    ) {
                        if (secretaryUiState is AutoCallSecretaryUiState.GeneratingReply) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = JarvisObsidian,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Kotib javob tayyorlamoqda...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Avtomatik Javob Berish (Ovoz + Bulut + SMS)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    when (secretaryUiState) {
                        is AutoCallSecretaryUiState.ReplyReady -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = JarvisObsidian.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, JarvisEmerald),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "AI KOTIB JAVOBI (${secretaryUiState.callerLabel}):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JarvisEmerald
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = secretaryUiState.spokenUzbekReply,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onSpeakText(secretaryUiState.spokenUzbekReply) },
                                            border = BorderStroke(1.dp, JarvisCyan)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = JarvisCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Qayta Eshitish", color = JarvisCyan)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onSendSmsReply(
                                                    secretaryUiState.callerNumber,
                                                    secretaryUiState.spokenUzbekReply
                                                )
                                            },
                                            border = BorderStroke(1.dp, JarvisAmberGold)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Sms,
                                                contentDescription = null,
                                                tint = JarvisAmberGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("SMS Jo'natish", color = JarvisAmberGold)
                                        }
                                    }
                                }
                            }
                        }
                        is AutoCallSecretaryUiState.Error -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = secretaryUiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = JarvisCrimson
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }

        // 4. Quick Cloud Voice Notes Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, JarvisViolet.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = JarvisViolet
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FIREBASE BULUT ESLATMALARI (${cloudVoiceNotes.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisViolet
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = noteTitleInput,
                            onValueChange = { noteTitleInput = it },
                            label = { Text("Sarlavha") },
                            singleLine = true,
                            modifier = Modifier.weight(0.4f)
                        )
                        OutlinedTextField(
                            value = noteContentInput,
                            onValueChange = { noteContentInput = it },
                            label = { Text("Eslatma matni") },
                            singleLine = true,
                            modifier = Modifier.weight(0.6f)
                        )
                        IconButton(
                            onClick = {
                                if (noteTitleInput.isNotBlank() || noteContentInput.isNotBlank()) {
                                    onCreateVoiceNote(
                                        noteTitleInput.ifBlank { "Ovozli Eslatma" },
                                        noteContentInput.ifBlank { noteTitleInput }
                                    )
                                    noteTitleInput = ""
                                    noteContentInput = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(JarvisViolet)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Saqlash",
                                tint = JarvisObsidian
                            )
                        }
                    }

                    if (cloudVoiceNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        cloudVoiceNotes.take(4).forEach { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(onClick = { onSpeakText("${note.title}. ${note.content}") }) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "O'qish",
                                            tint = JarvisCyan
                                        )
                                    }
                                    IconButton(onClick = { onDeleteVoiceNote(note.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "O'chirish",
                                            tint = JarvisCrimson
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // 5. Cloud Call Logs Header & Items
        item {
            Text(
                text = "AVTOMATIK JAVOB BERILGAN QO'NG'IROQLAR TARIXI (${cloudCallLogs.size})",
                style = MaterialTheme.typography.labelLarge,
                color = JarvisElectricBlue
            )
        }

        if (cloudCallLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Hozircha avtomatik javob berilgan qo'ng'iroqlar yo'q. Yuqoridagi tugma orqali AI Avto-Kotibni sinab ko'ring.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(cloudCallLogs, key = { it.id }) { log ->
                val dateFmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                val dateStr = dateFmt.format(log.createdAt?.toDate() ?: Date())

                Card(
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, JarvisElectricBlue.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${log.callerLabel} (${log.callerNumber})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Holat: ${log.statusPreset} • $dateStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = JarvisAmberGold
                                )
                            }

                            Row {
                                IconButton(onClick = { onSpeakText(log.aiGeneratedReply) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Eshitish",
                                        tint = JarvisCyan
                                    )
                                }
                                IconButton(
                                    onClick = { onSendSmsReply(log.callerNumber, log.aiGeneratedReply) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sms,
                                        contentDescription = "SMS Yuborish",
                                        tint = JarvisEmerald
                                    )
                                }
                                IconButton(onClick = { onDeleteCallLog(log.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "O'chirish",
                                        tint = JarvisCrimson
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = log.aiGeneratedReply,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisCyan
                        )
                    }
                }
            }
        }
    }
}

private fun requestCallScreeningRole(
    context: Context,
    onLaunchIntent: (android.content.Intent) -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                onLaunchIntent(intent)
            }
        }
    }
}
