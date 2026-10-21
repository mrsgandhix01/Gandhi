package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AiOperatingMode
import com.example.domain.AssistantTurnResult
import com.example.domain.SttState
import com.example.ui.PipelineStageStatus
import com.example.ui.components.PersistentStatusHeaderCard
import com.example.ui.theme.AshokaEmerald
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.KhadiIvory
import com.example.ui.theme.KhadiMuted
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SwarajSaffron

data class QuickBilingualCommand(
    val titleHindiOrEng: String,
    val subtitle: String,
    val fullCommand: String,
    val isHindi: Boolean
)

private val quickCommands = listOf(
    QuickBilingualCommand(
        titleHindiOrEng = "हे गांधी, बैकअप चलाओ",
        subtitle = "Termux: vault_backup.py",
        fullCommand = "हे गांधी, बैकअप चलाओ",
        isHindi = true
    ),
    QuickBilingualCommand(
        titleHindiOrEng = "सेटिंग्स खोलो",
        subtitle = "Launch Android Settings",
        fullCommand = "हे गांधी, सेटिंग्स खोलो",
        isHindi = true
    ),
    QuickBilingualCommand(
        titleHindiOrEng = "नेटवर्क स्कैन करो",
        subtitle = "Termux: lan_audit.py",
        fullCommand = "हे गांधी, नेटवर्क स्कैन करो",
        isHindi = true
    ),
    QuickBilingualCommand(
        titleHindiOrEng = "तुम मेरी प्राइवेसी कैसे बचाते हो?",
        subtitle = "Ask in Hindi (Devanagari)",
        fullCommand = "हे गांधी, तुम मेरी प्राइवेसी कैसे बचाते हो?",
        isHindi = true
    ),
    QuickBilingualCommand(
        titleHindiOrEng = "Clean temp files",
        subtitle = "Termux: purge_temp.py (English)",
        fullCommand = "Hey Gandhi, clean temp files",
        isHindi = false
    ),
    QuickBilingualCommand(
        titleHindiOrEng = "Open Calculator",
        subtitle = "Launch App (English)",
        fullCommand = "Hey Gandhi, open Calculator",
        isHindi = false
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantHubScreen(
    isServiceRunning: Boolean,
    liveRmsLevel: Float,
    framesProcessed: Long,
    wakeDetections: Int,
    sttState: SttState,
    partialTranscript: String,
    preferredSttLocale: String,
    ttsActiveLanguage: String,
    aiMode: AiOperatingMode,
    isGeminiKeyConfigured: Boolean,
    pipelineStage: PipelineStageStatus,
    latestTurnResult: AssistantTurnResult?,
    onToggleService: () -> Unit,
    onSelectAiMode: (AiOperatingMode) -> Unit,
    onSetSttLocale: (String) -> Unit,
    onRequestMicAndTriggerWake: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    onSpeakAgain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var customInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Persistent Status Header Card
        PersistentStatusHeaderCard(
            isServiceRunning = isServiceRunning,
            liveRmsLevel = liveRmsLevel,
            framesProcessed = framesProcessed,
            wakeDetections = wakeDetections,
            sttState = sttState,
            aiMode = aiMode,
            isGeminiKeyConfigured = isGeminiKeyConfigured,
            onToggleService = onToggleService,
            onSelectAiMode = onSelectAiMode,
            onTriggerWakeWord = onRequestMicAndTriggerWake
        )

        // 2. Wake-Word & Offline STT Trigger Controls
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Bilingual Engine",
                            tint = SwarajSaffron,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline STT & TTS Language Bias",
                            style = MaterialTheme.typography.titleMedium,
                            color = KhadiIvory
                        )
                    }

                    Text(
                        text = "TTS: $ttsActiveLanguage",
                        style = MaterialTheme.typography.labelSmall,
                        color = AshokaEmerald
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = preferredSttLocale == "hi-IN",
                        onClick = { onSetSttLocale("hi-IN") },
                        label = { Text("हिंदी-प्रथम (hi-IN • Preferred)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SwarajSaffron.copy(alpha = 0.2f),
                            selectedLabelColor = SwarajSaffron
                        ),
                        modifier = Modifier.testTag("stt_locale_hi_chip")
                    )
                    FilterChip(
                        selected = preferredSttLocale == "en-IN",
                        onClick = { onSetSttLocale("en-IN") },
                        label = { Text("English India (en-IN)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ChakraCyan.copy(alpha = 0.2f),
                            selectedLabelColor = ChakraCyan
                        ),
                        modifier = Modifier.testTag("stt_locale_en_chip")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRequestMicAndTriggerWake,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SwarajSaffron,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("say_hey_gandhi_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Speak Hey Gandhi",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (sttState == SttState.CAPTURING_COMMAND) "Listening Offline..." else "Say \"Hey Gandhi\"",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            onExecuteCommand("हे गांधी, मेरा एन्क्रिप्टेड बैकअप स्क्रिप्ट चलाओ")
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ObsidianSurfaceVariant,
                            contentColor = KhadiIvory
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("simulate_wake_word_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Simulate Wake Phrase",
                            tint = AshokaEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Wake")
                    }
                }

                if (partialTranscript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Live Offline STT Stream: \"$partialTranscript\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SwarajSaffron
                    )
                }
            }
        }

        // 3. 5-Stage Offline-First Pipeline Telemetry
        PipelineTelemetryBar(pipelineStage = pipelineStage)

        // 4. Latest Assistant Response & Action Execution Card
        if (latestTurnResult != null) {
            LatestTurnResponseCard(
                result = latestTurnResult,
                onSpeakAgain = { onSpeakAgain(latestTurnResult.assistantResponse) }
            )
        }

        // 5. Bilingual Quick Voice Simulators (Hindi & English)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "INSTANT BILINGUAL VOICE COMMANDS (TAP TO TRIGGER)",
                style = MaterialTheme.typography.labelSmall,
                color = KhadiMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickCommands.forEachIndexed { idx, cmd ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianSurface,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (cmd.isHindi) SwarajSaffron.copy(alpha = 0.45f) else ChakraCyan.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onExecuteCommand(cmd.fullCommand) }
                            .testTag("quick_command_chip_$idx")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (cmd.isHindi) SwarajSaffron else ChakraCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cmd.titleHindiOrEng,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = KhadiIvory,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = cmd.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = KhadiMuted
                            )
                        }
                    }
                }
            }
        }

        // 6. Direct Bilingual Command Bar (Hindi Devanagari / Hinglish / English)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(18.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it },
                    placeholder = {
                        Text(
                            text = "बोलें या लिखें: 'हे गांधी, सेटिंग्स खोलो' or 'Run backup.py'...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KhadiMuted
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SwarajSaffron,
                        unfocusedBorderColor = ObsidianCardBorder,
                        focusedTextColor = KhadiIvory,
                        unfocusedTextColor = KhadiIvory
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_command_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (customInput.isNotBlank()) {
                            onExecuteCommand(customInput)
                            customInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SwarajSaffron)
                        .testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Execute Command",
                        tint = ObsidianBg
                    )
                }
            }
        }
    }
}

@Composable
private fun PipelineTelemetryBar(pipelineStage: PipelineStageStatus) {
    val stages = listOf(
        "1. Wake-Word",
        "2. Offline STT",
        "3. Bilingual AI",
        "4. Action & TTS",
        "5. Local DB"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceVariant.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stages.forEachIndexed { idx, title ->
                    val stepNum = idx + 1
                    val isDoneOrActive = pipelineStage.activeStepIndex >= stepNum
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isDoneOrActive) {
                            AshokaEmerald.copy(alpha = 0.22f)
                        } else {
                            ObsidianSurface
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDoneOrActive) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AshokaEmerald,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDoneOrActive) AshokaEmerald else KhadiMuted,
                                fontWeight = if (isDoneOrActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = pipelineStage.stageNote,
                style = MaterialTheme.typography.bodyMedium,
                color = KhadiIvory
            )
        }
    }
}

@Composable
private fun LatestTurnResponseCard(
    result: AssistantTurnResult,
    onSpeakAgain: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SwarajSaffron.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .testTag("latest_response_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (result.detectedLanguage == "HI") {
                            SwarajSaffron.copy(alpha = 0.2f)
                        } else {
                            ChakraCyan.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = if (result.detectedLanguage == "HI") "हिंदी • HI-IN" else "ENGLISH • EN-IN",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (result.detectedLanguage == "HI") SwarajSaffron else ChakraCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "${result.latencyMs} ms • ${result.processingMode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AshokaEmerald
                    )
                }

                IconButton(
                    onClick = onSpeakAgain,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("replay_tts_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak Response with Offline TTS",
                        tint = SwarajSaffron
                    )
                }
            }

            Text(
                text = "You: \"${result.userQuery}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = KhadiMuted
            )

            Text(
                text = result.assistantResponse,
                style = MaterialTheme.typography.bodyLarge,
                color = KhadiIvory,
                fontWeight = FontWeight.Medium
            )

            if (result.executedAction != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AshokaEmerald.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Executed System Action",
                                tint = AshokaEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = result.executedAction,
                                style = MaterialTheme.typography.labelLarge,
                                color = AshokaEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!result.commandPreview.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$ ${result.commandPreview}",
                                style = MaterialTheme.typography.labelSmall,
                                color = KhadiMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
