package com.example.ui.screens

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DeviceTelemetrySnapshot
import com.example.model.TacticalProtocol
import com.example.ui.theme.JarvisAmberGold
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDeepNavy
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisSurfaceCard
import com.example.viewmodel.ProtocolUiState
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TelemetryRadarScreen(
    telemetry: DeviceTelemetrySnapshot,
    isTorchOn: Boolean,
    mediaVolumePercent: Int,
    protocolUiState: ProtocolUiState,
    onRefreshTelemetry: () -> Unit,
    onToggleTorch: (Boolean) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onLaunchAlarm: () -> Unit,
    onLaunchTimer: () -> Unit,
    onOpenSettings: (String, String) -> Unit,
    onExecuteProtocol: (TacticalProtocol) -> Unit,
    onSpeakText: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("telemetry_radar_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 360° Sensor Radar + Real-Time Accelerometer/Compass HUD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, JarvisEmerald.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = JarvisEmerald
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "AEGIS-GUARD // SENSOR RADAR & TELEMETRIYA",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = JarvisEmerald
                                )
                                Text(
                                    text = "Jonli apparat datchiklari, energiya va xotira skaneri",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshTelemetry,
                            modifier = Modifier.testTag("refresh_telemetry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh telemetry",
                                tint = JarvisEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier.size(185.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxRadius = size.minDimension / 2f

                            for (i in 1..3) {
                                drawCircle(
                                    color = JarvisEmerald.copy(alpha = 0.25f),
                                    radius = maxRadius * (i / 3f),
                                    center = center,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }

                            drawLine(
                                color = JarvisEmerald.copy(alpha = 0.28f),
                                start = Offset(0f, center.y),
                                end = Offset(size.width, center.y),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = JarvisEmerald.copy(alpha = 0.28f),
                                start = Offset(center.x, 0f),
                                end = Offset(center.x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )

                            rotate(degrees = radarSweepAngle, pivot = center) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        0.0f to Color.Transparent,
                                        0.75f to Color.Transparent,
                                        1.0f to JarvisEmerald.copy(alpha = 0.45f),
                                        center = center
                                    ),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = true
                                )
                                drawLine(
                                    color = JarvisEmerald,
                                    start = center,
                                    end = Offset(center.x + maxRadius, center.y),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                            val normX = (telemetry.accelX / 12f).coerceIn(-0.85f, 0.85f)
                            val normY = (telemetry.accelY / 12f).coerceIn(-0.85f, 0.85f)
                            val blipOffset = Offset(
                                x = center.x - normX * maxRadius * 0.75f,
                                y = center.y + normY * maxRadius * 0.75f
                            )
                            drawCircle(
                                color = JarvisCyan.copy(alpha = 0.35f),
                                radius = 12.dp.toPx(),
                                center = blipOffset
                            )
                            drawCircle(
                                color = JarvisCyan,
                                radius = 5.dp.toPx(),
                                center = blipOffset
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "G-FORCE: ${String.format(Locale.US, "%.2f", telemetry.gForceMagnitude)} m/s²",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisCyan
                        )
                        Text(
                            text = "AZIMUT: ${telemetry.azimuthDeg.roundToInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisEmerald
                        )
                        Text(
                            text = "XYZ: (${String.format(Locale.US, "%.1f", telemetry.accelX)}, ${String.format(Locale.US, "%.1f", telemetry.accelY)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Live Device Telemetry Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryStatCard(
                        title = "BATAREYA QUVVATI",
                        value = "${telemetry.batteryPercent}%",
                        subtitle = if (telemetry.isCharging) "Quvvat olmoqda • ${telemetry.voltageMv} mV"
                        else "Avtonom • ${telemetry.voltageMv} mV",
                        icon = Icons.Default.BatteryChargingFull,
                        accentColor = JarvisEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatCard(
                        title = "HARORAT DATCHIGI",
                        value = "${String.format(Locale.US, "%.1f", telemetry.temperatureCelsius)} °C",
                        subtitle = if (telemetry.temperatureCelsius < 38f) "Me'yoriy issiqlik" else "Yuqori yuklama",
                        icon = Icons.Default.Thermostat,
                        accentColor = JarvisAmberGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryStatCard(
                        title = "OPERATIV XOTIRA (RAM)",
                        value = "${telemetry.ramUsagePercent}%",
                        subtitle = "${telemetry.usedRamMb} / ${telemetry.totalRamMb} MB",
                        icon = Icons.Default.Memory,
                        accentColor = JarvisCyan,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatCard(
                        title = "DOIMIY XOTIRA (ROM)",
                        value = "${String.format(Locale.US, "%.1f", telemetry.freeStorageGb)} GB",
                        subtitle = "Bo'sh (Jami ${String.format(Locale.US, "%.1f", telemetry.totalStorageGb)} GB)",
                        icon = Icons.Default.Storage,
                        accentColor = JarvisElectricBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Hands-Free & Manual Phone Hardware Control Deck
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TELEFON APPARAT BOSHQARUV PANELI",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisCyan
                    )
                    Text(
                        text = "Ovozda yoki tugmalar orqali telefon funksiyalarini bevosita boshqaring",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onToggleTorch(!isTorchOn) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTorchOn) JarvisAmberGold else JarvisDeepNavy,
                                contentColor = if (isTorchOn) Color(0xFF1F1400) else JarvisCyan
                            ),
                            border = BorderStroke(1.dp, if (isTorchOn) JarvisAmberGold else JarvisCyan),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("torch_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Toggle Flashlight",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTorchOn) "FONARIK ON" else "FONARIK OFF",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onLaunchAlarm,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, JarvisElectricBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("quick_alarm_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessAlarm,
                                contentDescription = null,
                                tint = JarvisElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Budilnik", color = JarvisElectricBlue)
                        }

                        OutlinedButton(
                            onClick = onLaunchTimer,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, JarvisEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("quick_timer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = JarvisEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Taymer", color = JarvisEmerald)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MEDIA OVOZ BALANDLIGI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$mediaVolumePercent%",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisCyan
                        )
                    }

                    Slider(
                        value = mediaVolumePercent.toFloat(),
                        onValueChange = { onVolumeChange(it.roundToInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyan,
                            activeTrackColor = JarvisCyan
                        ),
                        modifier = Modifier.testTag("volume_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onOpenSettings(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi sozlamalari ochildi") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wi-Fi", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = { onOpenSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth sozlamalari ochildi") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bluetooth", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = { onOpenSettings(Settings.ACTION_DISPLAY_SETTINGS, "Displey sozlamalari ochildi") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.SettingsBrightness, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ekran", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 4. Tactical Voice Protocols & Executive Briefing Section
        item {
            Text(
                text = "AVTONOM OVOZLI PROTOKOLLAR VA KUNLIK BRIFING",
                style = MaterialTheme.typography.labelLarge,
                color = JarvisAmberGold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Protokolni bosing — JARVIS telefon sozlamalarini moslashtiradi va real telemetriya asosida ovozli hisobot beradi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TacticalProtocol.entries.forEach { protocol ->
                    val isRunningThis = protocolUiState is ProtocolUiState.Running &&
                        protocolUiState.protocol == protocol

                    Card(
                        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, protocol.accentColor.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = protocolUiState !is ProtocolUiState.Running) {
                                onExecuteProtocol(protocol)
                            }
                            .testTag("protocol_card_${protocol.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(protocol.accentColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isRunningThis) {
                                        CircularProgressIndicator(
                                            color = protocol.accentColor,
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = protocol.accentColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = protocol.codeName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = protocol.accentColor
                                    )
                                    Text(
                                        text = protocol.titleUz,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = protocol.subtitleUz,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Execute Protocol",
                                tint = protocol.accentColor
                            )
                        }
                    }
                }
            }
        }

        // 5. Active Protocol Report Output
        when (protocolUiState) {
            is ProtocolUiState.Idle -> Unit
            is ProtocolUiState.Running -> Unit
            is ProtocolUiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = JarvisCrimson.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, JarvisCrimson)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "PROTOKOL XATOLIGI",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisCrimson
                            )
                            Text(
                                text = protocolUiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            is ProtocolUiState.ActiveReport -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, protocolUiState.protocol.accentColor),
                        modifier = Modifier.testTag("active_protocol_report_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${protocolUiState.protocol.codeName} // FAOLLASHTIRILDI",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = protocolUiState.protocol.accentColor
                                    )
                                    Text(
                                        text = protocolUiState.hardwareActionTaken,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JarvisEmerald
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onSpeakText(protocolUiState.spokenBriefing) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, protocolUiState.protocol.accentColor)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = protocolUiState.protocol.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "O'qish",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = protocolUiState.protocol.accentColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = protocolUiState.spokenBriefing,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            protocolUiState.systemRecommendations.forEach { rec ->
                                Row(
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = protocolUiState.protocol.accentColor,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rec,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
