package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.example.model.VisionAnalysisMode
import com.example.model.VisionHudResult
import com.example.ui.theme.JarvisAmberGold
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisViolet
import com.example.viewmodel.VisionUiState

@Composable
fun VisionHudScreen(
    uiState: VisionUiState,
    selectedBitmap: Bitmap?,
    onSelectBitmap: (Bitmap) -> Unit,
    onAnalyzeImage: (Bitmap, VisionAnalysisMode, String) -> Unit,
    onSpeakText: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    var selectedMode by rememberSaveable { mutableStateOf(VisionAnalysisMode.AUTO_DIAGNOSTICS) }
    var customQuestion by rememberSaveable { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                onSelectBitmap(bmp)
            } catch (_: Exception) {
                onSelectBitmap(createSampleDiagnosticBitmap())
            }
        }
    }

    val isAnalyzing = uiState is VisionUiState.Analyzing
    val infiniteTransition = rememberInfiniteTransition(label = "vision_scan")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("vision_hud_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Vision HUD Header & Viewfinder Frame
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, JarvisViolet.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = JarvisViolet
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "VISION-NOVA // MULTIMODAL HUD",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = JarvisViolet
                                )
                                Text(
                                    text = "Obyekt, Kod skrinshoti, Hujjat va Sxema tahlili",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = JarvisViolet.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "GEMINI 3.8 VISION",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisViolet,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Viewfinder Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF060E1E)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap.asImageBitmap(),
                                contentDescription = "Selected image for Vision HUD",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = null,
                                    tint = JarvisCyan.copy(alpha = 0.75f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tahlil uchun rasm tanlang yoki Namunaviy Sxemani yuklang",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Tactical HUD Corners & Laser Scanline Overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cornerLen = 24.dp.toPx()
                            val pad = 12.dp.toPx()
                            val stroke = 2.5.dp.toPx()
                            val hudColor = if (isAnalyzing) JarvisAmberGold else JarvisCyan

                            // Top-Left corner
                            drawLine(hudColor, Offset(pad, pad), Offset(pad + cornerLen, pad), stroke)
                            drawLine(hudColor, Offset(pad, pad), Offset(pad, pad + cornerLen), stroke)
                            // Top-Right corner
                            drawLine(hudColor, Offset(size.width - pad, pad), Offset(size.width - pad - cornerLen, pad), stroke)
                            drawLine(hudColor, Offset(size.width - pad, pad), Offset(size.width - pad, pad + cornerLen), stroke)
                            // Bottom-Left corner
                            drawLine(hudColor, Offset(pad, size.height - pad), Offset(pad + cornerLen, size.height - pad), stroke)
                            drawLine(hudColor, Offset(pad, size.height - pad), Offset(pad, size.height - pad - cornerLen), stroke)
                            // Bottom-Right corner
                            drawLine(hudColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - cornerLen, size.height - pad), stroke)
                            drawLine(hudColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - cornerLen), stroke)

                            if (selectedBitmap != null || isAnalyzing) {
                                val scanY = size.height * scanLineProgress
                                drawLine(
                                    color = hudColor.copy(alpha = 0.85f),
                                    start = Offset(pad, scanY),
                                    end = Offset(size.width - pad, scanY),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Image Selection Buttons (Photo Picker + Sample Diagnostic Frame)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, JarvisCyan),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galereya", color = JarvisCyan)
                        }

                        OutlinedButton(
                            onClick = {
                                onSelectBitmap(createSampleDiagnosticBitmap())
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, JarvisViolet),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("sample_vision_frame_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = JarvisViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Namunaviy Kadr", color = JarvisViolet)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Vision Analysis Mode Chips
                    Text(
                        text = "TAHLIL YO'NALISHI:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VisionAnalysisMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode.labelUz) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = JarvisViolet.copy(alpha = 0.24f),
                                    selectedLabelColor = JarvisViolet
                                ),
                                modifier = Modifier.testTag("vision_mode_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customQuestion,
                        onValueChange = { customQuestion = it },
                        placeholder = {
                            Text(
                                text = "Rasm bo'yicha qo'shimcha savol (ixtiyoriy)...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisViolet,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vision_question_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val bmpToAnalyze = selectedBitmap ?: createSampleDiagnosticBitmap().also {
                                onSelectBitmap(it)
                            }
                            onAnalyzeImage(bmpToAnalyze, selectedMode, customQuestion)
                        },
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisViolet,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_vision_scan_button")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "VISION-NOVA Tahlil Qilmoqda...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(imageVector = Icons.Default.CenterFocusStrong, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vizual AI Diagnostikani Boshlash",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Vision Analysis Output
        when (uiState) {
            is VisionUiState.Idle -> Unit
            is VisionUiState.Analyzing -> Unit
            is VisionUiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = JarvisCrimson.copy(alpha = 0.16f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, JarvisCrimson)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "VISION HUD XATOLIGI",
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
            is VisionUiState.Success -> {
                item {
                    VisionResultSection(
                        result = uiState.result,
                        onSpeakSummary = { onSpeakText(uiState.result.voiceSummary) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VisionResultSection(
    result: VisionHudResult,
    onSpeakSummary: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.testTag("vision_result_section")
    ) {
        // 1. Voice Summary & Detected Object Card
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, JarvisViolet)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ANIQLANGAN OBYEKT: ${result.detectedCategory.uppercase()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisViolet
                        )
                        Text(
                            text = "${result.modeLabel} • Aniqlik: ${result.confidenceScore}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisEmerald
                        )
                    }

                    OutlinedButton(
                        onClick = onSpeakSummary,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, JarvisViolet)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = JarvisViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("O'qish", style = MaterialTheme.typography.labelSmall, color = JarvisViolet)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = result.voiceSummary,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 2. Key Findings
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ASOSIY VIZUAL TOPILMALAR:",
                    style = MaterialTheme.typography.labelLarge,
                    color = JarvisCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                result.keyFindings.forEach { finding ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = finding,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. Technical Deep Dive
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, JarvisElectricBlue.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = JarvisElectricBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHUQUR TEXNIK & KOD TAHLILI:",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisElectricBlue
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.technicalDeepDive,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 4. Recommended Actions
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, JarvisAmberGold.copy(alpha = 0.65f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TAVSIYA ETILGAN AMALLAR:",
                    style = MaterialTheme.typography.labelLarge,
                    color = JarvisAmberGold
                )
                Spacer(modifier = Modifier.height(8.dp))
                result.recommendedActions.forEachIndexed { idx, act ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = act,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun createSampleDiagnosticBitmap(): Bitmap {
    val width = 800
    val height = 480
    val bitmap = createBitmap(width, height)
    val canvas = AndroidCanvas(bitmap)

    canvas.drawColor("#071124".toColorInt())

    val gridPaint = Paint().apply {
        color = "#132A4A".toColorInt()
        strokeWidth = 2f
    }
    for (x in 0..width step 40) {
        canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
    }
    for (y in 0..height step 40) {
        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
    }

    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = "#00E5FF".toColorInt()
    }
    canvas.drawRect(40f, 50f, 360f, 210f, boxPaint)
    canvas.drawRect(440f, 50f, 760f, 210f, boxPaint)
    boxPaint.color = "#FFB300".toColorInt()
    canvas.drawRect(180f, 280f, 620f, 430f, boxPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#00E5FF".toColorInt()
        textSize = 24f
        isFakeBoldText = true
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#E6F4FF".toColorInt()
        textSize = 20f
    }
    val warnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FFB300".toColorInt()
        textSize = 21f
        isFakeBoldText = true
    }

    canvas.drawText("MODULE A: VOICE STT PIPELINE", 60f, 95f, titlePaint)
    canvas.drawText("Latency: 180ms | SampleRate: 16kHz", 60f, 135f, textPaint)
    canvas.drawText("Status: ACTIVE (Full-Duplex)", 60f, 175f, textPaint)

    canvas.drawText("MODULE B: MULTI-AGENT ROUTER", 460f, 95f, titlePaint)
    canvas.drawText("Model: Gemini 3.8 Flash", 460f, 135f, textPaint)
    canvas.drawText("Agents: 5 Parallel Workers", 460f, 175f, textPaint)

    canvas.drawText("DIAGNOSTIC ALERT: MEMORY & CACHE", 205f, 325f, warnPaint)
    canvas.drawText("fun optimizeVectorCache(maxItems: Int = 500)", 205f, 365f, textPaint)
    canvas.drawText("TODO: Add LRU eviction & encryption", 205f, 405f, textPaint)

    return bitmap
}
