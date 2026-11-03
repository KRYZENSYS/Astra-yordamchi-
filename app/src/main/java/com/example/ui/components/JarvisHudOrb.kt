package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AiAgentRole
import com.example.ui.theme.JarvisAmberGold
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDeepNavy
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisSurfaceCard
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisHudVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    voiceRmsLevel: Float,
    selectedAgent: AiAgentRole?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_transition")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isProcessing) 4000 else 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isProcessing) 3000 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    isListening || isSpeaking -> 450
                    isProcessing -> 650
                    else -> 1800
                }
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val primaryHudColor = when {
        isListening -> JarvisEmerald
        isProcessing -> JarvisAmberGold
        isSpeaking -> JarvisCyan
        selectedAgent != null -> selectedAgent.accentColor
        else -> JarvisCyan
    }

    val statusLabel = when {
        isListening -> "AUDIO INPUT ACTIVE // TINGLANMOQDA"
        isProcessing -> "MULTI-AI SWARM PROCESSING // TAHLIL"
        isSpeaking -> "VOICE SYNTHESIS ACTIVE // OVOZLI JAVOB"
        selectedAgent != null -> "${selectedAgent.codeName} // ONLINE"
        else -> "5-AI NEXUS COUNCIL // READY"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        JarvisSurfaceCard.copy(alpha = 0.95f),
                        JarvisDeepNavy.copy(alpha = 0.98f)
                    )
                )
            )
            .border(1.dp, primaryHudColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(primaryHudColor)
                )
                Text(
                    text = "  $statusLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryHudColor
                )
            }
            Text(
                text = "CORE v4.8",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f

                // Ambient radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryHudColor.copy(alpha = 0.28f * pulseScale),
                            primaryHudColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    ),
                    radius = maxRadius,
                    center = center
                )

                // Outer segmented tactical ring
                rotate(degrees = outerRotation, pivot = center) {
                    val arcRadius = maxRadius * 0.88f
                    val arcSize = Size(arcRadius * 2, arcRadius * 2)
                    val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                    for (i in 0 until 6) {
                        drawArc(
                            color = primaryHudColor.copy(alpha = 0.65f),
                            startAngle = i * 60f + 8f,
                            sweepAngle = 38f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Orbiting 5 AI Agent Nodes
                val agents = AiAgentRole.entries
                val orbitRadius = maxRadius * 0.72f
                agents.forEachIndexed { index, agent ->
                    val angleDeg = (index * 72f) + (outerRotation * 0.5f)
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val nodeX = center.x + (orbitRadius * cos(angleRad)).toFloat()
                    val nodeY = center.y + (orbitRadius * sin(angleRad)).toFloat()
                    val nodeOffset = Offset(nodeX, nodeY)

                    // Neural link line to core
                    drawLine(
                        color = agent.accentColor.copy(alpha = if (isProcessing || selectedAgent == null || selectedAgent == agent) 0.45f else 0.15f),
                        start = center,
                        end = nodeOffset,
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Agent satellite node
                    drawCircle(
                        color = agent.accentColor,
                        radius = if (selectedAgent == agent || isProcessing) 6.dp.toPx() else 4.5.dp.toPx(),
                        center = nodeOffset
                    )
                }

                // Inner counter-rotating ring
                rotate(degrees = innerRotation, pivot = center) {
                    val innerRadius = maxRadius * 0.52f
                    val arcSize = Size(innerRadius * 2, innerRadius * 2)
                    val topLeft = Offset(center.x - innerRadius, center.y - innerRadius)
                    for (i in 0 until 4) {
                        drawArc(
                            color = JarvisElectricBlue.copy(alpha = 0.8f),
                            startAngle = i * 90f + 15f,
                            sweepAngle = 55f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Central Arc-Reactor Core
                val dynamicBoost = if (isListening) (voiceRmsLevel * 14f) else 0f
                val coreRadius = (maxRadius * 0.28f * pulseScale) + dynamicBoost
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            primaryHudColor,
                            primaryHudColor.copy(alpha = 0.2f)
                        ),
                        center = center,
                        radius = coreRadius
                    ),
                    radius = coreRadius,
                    center = center
                )

                // Acoustic waveform bars across the center
                val barCount = 9
                val barSpacing = 6.dp.toPx()
                val totalWidth = (barCount - 1) * barSpacing
                val startX = center.x - totalWidth / 2f
                for (i in 0 until barCount) {
                    val distanceFromCenter = kotlin.math.abs(i - barCount / 2)
                    val activityFactor = when {
                        isListening -> (0.4f + voiceRmsLevel * 1.4f)
                        isSpeaking || isProcessing -> pulseScale
                        else -> 0.3f
                    }
                    val barHeight = (22.dp.toPx() - distanceFromCenter * 3.dp.toPx()) * activityFactor
                    val x = startX + i * barSpacing
                    drawLine(
                        color = Color.White.copy(alpha = 0.9f),
                        start = Offset(x, center.y - barHeight / 2f),
                        end = Offset(x, center.y + barHeight / 2f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5 AI Agent Status Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AiAgentRole.entries.forEach { agent ->
                val isActive = selectedAgent == null || selectedAgent == agent
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isActive) agent.accentColor.copy(alpha = 0.14f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isActive) agent.accentColor else agent.accentColor.copy(alpha = 0.3f))
                    )
                    Text(
                        text = " ${agent.name.take(4)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) agent.accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
