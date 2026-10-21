package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AiOperatingMode
import com.example.domain.SttState
import com.example.ui.theme.AshokaEmerald
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.KhadiIvory
import com.example.ui.theme.KhadiMuted
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SwarajSaffron
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PersistentStatusHeaderCard(
    isServiceRunning: Boolean,
    liveRmsLevel: Float,
    framesProcessed: Long,
    wakeDetections: Int,
    sttState: SttState,
    aiMode: AiOperatingMode,
    isGeminiKeyConfigured: Boolean,
    onToggleService: () -> Unit,
    onSelectAiMode: (AiOperatingMode) -> Unit,
    onTriggerWakeWord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "charkha_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radius"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isServiceRunning) {
                        listOf(SwarajSaffron.copy(alpha = 0.7f), AshokaEmerald.copy(alpha = 0.6f))
                    } else {
                        listOf(ObsidianCardBorder, ObsidianCardBorder)
                    }
                ),
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Privacy Shield Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AshokaEmerald.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Zero Login Privacy",
                        tint = AshokaEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ZERO-LOGIN • 100% OFFLINE AUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = AshokaEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Wake Hits: $wakeDetections",
                    style = MaterialTheme.typography.labelSmall,
                    color = SwarajSaffron
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Row: Charkha Acoustic Visualizer Orb + Service Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Acoustic Charkha Orb
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .clickable { onTriggerWakeWord() }
                        .testTag("wake_word_orb_button")
                ) {
                    Canvas(modifier = Modifier.size(76.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = size.minDimension / 2.6f
                        val activeBoost = if (sttState != SttState.IDLE) 1.25f else 1.0f
                        val effectiveRadius = baseRadius * pulseRadius * activeBoost

                        // Outer Glow Ring
                        drawCircle(
                            color = if (isServiceRunning) SwarajSaffron.copy(alpha = 0.18f) else KhadiMuted.copy(alpha = 0.1f),
                            radius = effectiveRadius
                        )

                        // Charkha 12-Spoke Acoustic Wheel
                        val spokes = 12
                        for (i in 0 until spokes) {
                            val angle = (i * (360f / spokes) * (Math.PI / 180.0)) + phase * 0.25
                            val innerR = baseRadius * 0.35f
                            val waveMod = sin(phase + i).toFloat() * (liveRmsLevel * 12f + 3f)
                            val outerR = (baseRadius * 0.85f + waveMod).coerceAtMost(size.minDimension / 2f - 2f)
                            val start = Offset(
                                x = center.x + (innerR * cos(angle)).toFloat(),
                                y = center.y + (innerR * sin(angle)).toFloat()
                            )
                            val end = Offset(
                                x = center.x + (outerR * cos(angle)).toFloat(),
                                y = center.y + (outerR * sin(angle)).toFloat()
                            )
                            drawLine(
                                color = if (isServiceRunning) SwarajSaffron.copy(alpha = 0.75f) else KhadiMuted.copy(alpha = 0.3f),
                                start = start,
                                end = end,
                                strokeWidth = 2.5f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Core Ring
                        drawCircle(
                            color = if (isServiceRunning) AshokaEmerald else KhadiMuted,
                            radius = baseRadius * 0.56f,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    Icon(
                        imageVector = if (isServiceRunning) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Wake Word Microphone",
                        tint = if (isServiceRunning) KhadiIvory else KhadiMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hey Gandhi • हे गांधी",
                        style = MaterialTheme.typography.titleLarge,
                        color = KhadiIvory
                    )
                    Text(
                        text = if (isServiceRunning) {
                            "Foreground Service Active • 16kHz PCM (${framesProcessed} frames purged)"
                        } else {
                            "Background Wake-Word Service Paused"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isServiceRunning) AshokaEmerald else KhadiMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "हिंदी-प्रथम द्विभाषी सहायक (Hindi-First Bilingual)",
                        style = MaterialTheme.typography.labelSmall,
                        color = SwarajSaffron
                    )
                }

                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = { onToggleService() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianSurface,
                        checkedTrackColor = SwarajSaffron,
                        uncheckedThumbColor = KhadiMuted,
                        uncheckedTrackColor = ObsidianSurfaceVariant
                    ),
                    modifier = Modifier.testTag("wake_service_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector: Strict Offline vs Hybrid (Offline Audio + Gemini API)
            Text(
                text = "AI ROUTER MODE (NO LOGIN REQUIRED)",
                style = MaterialTheme.typography.labelSmall,
                color = KhadiMuted
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = aiMode == AiOperatingMode.STRICT_OFFLINE,
                    onClick = { onSelectAiMode(AiOperatingMode.STRICT_OFFLINE) },
                    label = {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "Strict Offline",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "100% On-Device NLU",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Strict Offline Mode",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AshokaEmerald.copy(alpha = 0.22f),
                        selectedLabelColor = AshokaEmerald,
                        selectedLeadingIconColor = AshokaEmerald
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_strict_offline_chip")
                )

                FilterChip(
                    selected = aiMode == AiOperatingMode.HYBRID_GEMINI,
                    onClick = { onSelectAiMode(AiOperatingMode.HYBRID_GEMINI) },
                    label = {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "Hybrid Gemini",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isGeminiKeyConfigured) "Offline STT + Gemini 3.5" else "Offline STT + Local Fallback",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Hybrid Gemini Mode",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SwarajSaffron.copy(alpha = 0.22f),
                        selectedLabelColor = SwarajSaffron,
                        selectedLeadingIconColor = SwarajSaffron
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_hybrid_gemini_chip")
                )
            }
        }
    }
}
