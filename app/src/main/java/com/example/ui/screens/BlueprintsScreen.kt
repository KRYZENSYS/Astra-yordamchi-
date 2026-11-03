package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.JarvisBlueprintIdea
import com.example.ui.theme.JarvisAmberGold
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisViolet
import com.example.viewmodel.BlueprintGenState

@Composable
fun BlueprintsScreen(
    blueprints: List<JarvisBlueprintIdea>,
    genState: BlueprintGenState,
    onGenerateBlueprint: (String) -> Unit,
    onSpeakBlueprint: (JarvisBlueprintIdea) -> Unit,
    onExploreInCouncil: (JarvisBlueprintIdea) -> Unit,
    onDeleteCustomBlueprint: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    var topicInput by rememberSaveable { mutableStateOf("") }
    var expandedBlueprintId by rememberSaveable { mutableStateOf(blueprints.firstOrNull()?.id.orEmpty()) }

    val quickTopics = listOf(
        "Aqlli Uy va Xavfsizlik Jarvis",
        "Full-Stack Dasturchi Kod-Jarvis",
        "Avtomobil va Yo'l Boshqaruvchisi",
        "Shaxsiy Tibbiy va Fitness AI",
        "Biznes va Moliya Direktori Jarvis"
    )

    val isGenerating = genState is BlueprintGenState.Generating

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("blueprints_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. AI Blueprint Generator Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, JarvisCyan.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = JarvisCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "MULTI-AI JARVIS LOYIHA GENERATORI",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisCyan
                            )
                            Text(
                                text = "Istalgan soha uchun ovozli + ko'p-agentli Jarvis arxitekturasini yarating",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.placeholder_blueprint_topic),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("blueprint_topic_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickTopics.forEachIndexed { index, topic ->
                            AssistChip(
                                onClick = {
                                    topicInput = topic
                                    if (!isGenerating) {
                                        onGenerateBlueprint(topic)
                                    }
                                },
                                label = { Text(topic, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = JarvisElectricBlue
                                ),
                                modifier = Modifier.testTag("quick_blueprint_chip_$index")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (topicInput.isNotBlank()) {
                                onGenerateBlueprint(topicInput)
                            }
                        },
                        enabled = topicInput.isNotBlank() && !isGenerating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = Color(0xFF002029)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_blueprint_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = Color(0xFF002029),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "AI Arxitektura Yaratilmoqda...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Build, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_generate_blueprint),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (genState is BlueprintGenState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = genState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisCrimson
                        )
                    }
                }
            }
        }

        // 2. Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MUKAMMAL JARVIS ARXITEKTURALARI (${blueprints.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = JarvisAmberGold
                )
                Text(
                    text = "Batafsil ko'rish uchun bosing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. Blueprint Cards
        items(blueprints, key = { it.id }) { blueprint ->
            val isExpanded = expandedBlueprintId == blueprint.id ||
                (genState is BlueprintGenState.Success && genState.blueprint.id == blueprint.id)

            BlueprintDetailCard(
                blueprint = blueprint,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedBlueprintId = if (expandedBlueprintId == blueprint.id) "" else blueprint.id
                },
                onSpeak = { onSpeakBlueprint(blueprint) },
                onExploreInCouncil = { onExploreInCouncil(blueprint) },
                onDeleteCustom = if (blueprint.id.startsWith("custom_")) {
                    { onDeleteCustomBlueprint(blueprint.id) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun BlueprintDetailCard(
    blueprint: JarvisBlueprintIdea,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSpeak: () -> Unit,
    onExploreInCouncil: () -> Unit,
    onDeleteCustom: (() -> Unit)?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) JarvisCyan else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onToggleExpand() }
            .testTag("blueprint_card_${blueprint.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = JarvisCyan.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = blueprint.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = JarvisEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "POWER ${blueprint.powerLevel}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisEmerald
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Ovozda o'qish",
                            tint = JarvisCyan
                        )
                    }
                    if (onDeleteCustom != null) {
                        IconButton(
                            onClick = onDeleteCustom,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete custom blueprint",
                                tint = JarvisCrimson
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = blueprint.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, JarvisAmberGold.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = JarvisAmberGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "OVOZLI ZANJIR (VOICE PIPELINE):",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisAmberGold
                        )
                        Text(
                            text = blueprint.voicePipeline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ISHTIROK ETUVCHI SUN'IY INTELLEKT MODELLARI:",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisElectricBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    blueprint.aiModelsUsed.forEach { modelInfo ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = JarvisElectricBlue,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = modelInfo,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ASOSIY IMKONIYATLAR VA SUPER-FUNKSIYALAR:",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisViolet
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    blueprint.coreFeatures.forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(JarvisViolet)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "QURISH VA ISHGA TUSHIRISH BOSQICHLARI:",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisEmerald
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    blueprint.implementationSteps.forEach { step ->
                        Surface(
                            color = JarvisEmerald.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSpeak,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, JarvisCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ovozda Eshitish", color = JarvisCyan)
                        }

                        Button(
                            onClick = onExploreInCouncil,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisElectricBlue,
                                contentColor = Color(0xFF001E2F)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kengashga Yuborish", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
