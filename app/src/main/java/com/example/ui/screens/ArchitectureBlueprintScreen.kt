package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AshokaEmerald
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.KhadiIvory
import com.example.ui.theme.KhadiMuted
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SwarajSaffron

@Composable
fun ArchitectureBlueprintScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("architecture_blueprint_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hybrid / Offline-First AI Router Resolution
        BlueprintCard(
            badge = "ARCHITECTURAL RESOLUTION",
            title = "Hybrid / Offline-First Privacy Router",
            subtitle = "Reconciling 100% Offline Privacy with Gemini API Intelligence",
            codeOrDiagram = """
• Mode 1: Strict Offline Mode (100% On-Device)
  Zero network packets. Uses local bilingual Hindi/English intent parser + on-device LLM/NLU (Gemini Nano via AICore / Gemma 2B ONNX).

• Mode 2: Privacy-Preserving Hybrid Mode
  100% of microphone audio, wake-word detection ("Hey Gandhi"), and Speech-to-Text run strictly offline. Raw PCM audio frames are zeroed out in RAM immediately. Only stripped, anonymous text strings reach Gemini 3.5 Flash with zero account login or email tracking.
            """.trimIndent()
        )

        // 2. End-to-End 5-Stage Pipeline Diagram
        BlueprintCard(
            badge = "CORE PIPELINE WORKFLOW",
            title = "5-Stage Voice-to-Action Pipeline",
            subtitle = "16kHz PCM AudioRecord -> Wake-Word -> Offline STT -> Gemini Function Call -> Local Room DB",
            codeOrDiagram = """
[Microphone 16kHz PCM Stream]
       │
       ▼
[1. Wake-Word Engine (Offline Porcupine / OpenWakeWord ONNX)]
       │  (Detects "Hey Gandhi" / "हे गांधी")
       ▼
[2. Offline STT Engine (Sherpa-ONNX Whisper / Vosk hi-IN & en-IN)]
       │  (Converts Audio -> Text locally; Audio buffer zeroed)
       ▼
[3. Gemini 3.5 Flash / Local Bilingual NLU Router]
       ├──► [FunctionCall: launchApp] ──► PackageManager Intent
       ├──► [FunctionCall: runTermuxScript] ──► com.termux.RUN_COMMAND
       └──► [Bilingual Response (Devanagari Hindi-First)]
                               │
                               ▼
                    [4. Offline TTS (hi-IN / en-IN)]
                               │
                               ▼
                    [5. Local Room Vault (TranscriptEntity)]
            """.trimIndent()
        )

        // 3. Bilingual Unicode & TTS Routing Spec
        BlueprintCard(
            badge = "BILINGUAL HINDI-FIRST ENGINE",
            title = "Devanagari Script & Locale Switching",
            subtitle = "Automatic Unicode range inspection (\\u0900–\\u097F)",
            codeOrDiagram = """
val containsDevanagari = responseText.any { it in '\u0900'..'\u097F' }
tts.language = if (containsDevanagari) {
    Locale("hi", "IN") // Pure Hindi offline neural voice pack
} else {
    Locale("en", "IN") // Indian-accented English voice pack
}
            """.trimIndent()
        )

        // 4. Standalone APK Packaging Spec
        BlueprintCard(
            badge = "STANDALONE APK PACKAGING",
            title = "Uncompressed ONNX/TFLite & ABI Config",
            subtitle = "Configured in app/build.gradle.kts for direct memory-mapping",
            codeOrDiagram = """
android {
    androidResources {
        // Prevents AAPT2 from compressing bundled wake-word & STT models
        noCompress += listOf("onnx", "tflite", "ppn", "bin")
    }
}
// Build standalone installable APK:
// gradle assembleRelease
            """.trimIndent()
        )
    }
}

@Composable
private fun BlueprintCard(
    badge: String,
    title: String,
    subtitle: String,
    codeOrDiagram: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AshokaEmerald,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = SwarajSaffron,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = KhadiIvory
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = KhadiMuted
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = codeOrDiagram,
                    style = MaterialTheme.typography.labelSmall,
                    color = ChakraCyan,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
