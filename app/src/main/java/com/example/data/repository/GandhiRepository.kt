package com.example.data.repository

import com.example.data.local.GandhiDao
import com.example.data.local.TermuxScriptEntity
import com.example.data.local.TranscriptEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GandhiRepository(private val dao: GandhiDao) {

    val allTranscripts: Flow<List<TranscriptEntity>> = dao.getAllTranscripts()
    val allTermuxScripts: Flow<List<TermuxScriptEntity>> = dao.getAllTermuxScripts()

    private val prettyJson = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun ensureSeedData() {
        if (dao.getTermuxScriptCount() == 0) {
            val defaultScripts = listOf(
                TermuxScriptEntity(
                    aliasHindi = "बैकअप चलाओ",
                    aliasEnglish = "run encrypted backup",
                    scriptFileName = "vault_backup.py",
                    defaultArgs = "--target /sdcard/GandhiVault --cipher aes256",
                    description = "Encrypts local documents and photos into an offline archive via Termux Python.",
                    runInBackground = true,
                    executionCount = 4
                ),
                TermuxScriptEntity(
                    aliasHindi = "नेटवर्क स्कैन करो",
                    aliasEnglish = "scan local network",
                    scriptFileName = "lan_audit.py",
                    defaultArgs = "--subnet 192.168.1.0/24 --quiet",
                    description = "Audits connected Wi-Fi devices and checks for open ports locally.",
                    runInBackground = true,
                    executionCount = 2
                ),
                TermuxScriptEntity(
                    aliasHindi = "कैश साफ करो",
                    aliasEnglish = "clean temp files",
                    scriptFileName = "purge_temp.py",
                    defaultArgs = "--deep-clean --keep-logs 0",
                    description = "Removes residual cache, EXIF metadata, and clipboard history via Python.",
                    runInBackground = true,
                    executionCount = 7
                ),
                TermuxScriptEntity(
                    aliasHindi = "सिस्टम स्थिति बताओ",
                    aliasEnglish = "check battery telemetry",
                    scriptFileName = "sys_health.py",
                    defaultArgs = "--sensors --thermal",
                    description = "Reads CPU thermal zones and battery discharge curves via Termux-API.",
                    runInBackground = false,
                    executionCount = 3
                )
            )
            defaultScripts.forEach { dao.insertTermuxScript(it) }
        }

        if (dao.getTranscriptCount() == 0) {
            val now = System.currentTimeMillis()
            val sampleTranscripts = listOf(
                TranscriptEntity(
                    timestamp = now - 3_600_000L,
                    detectedLanguage = "HI",
                    userQuery = "हे गांधी, मेरा एन्क्रिप्टेड बैकअप स्क्रिप्ट चलाओ",
                    assistantResponse = "जी ज़रूर! मैंने टर्मक्स में 'vault_backup.py' स्क्रिप्ट को सुरक्षित रूप से पृष्ठभूमि (background) में शुरू कर दिया है। आपका सारा डेटा डिवाइस पर ही सुरक्षित है।",
                    executedAction = "Termux: vault_backup.py --target /sdcard/GandhiVault --cipher aes256",
                    processingMode = "STRICT_OFFLINE",
                    latencyMs = 95L,
                    isPinned = true
                ),
                TranscriptEntity(
                    timestamp = now - 1_800_000L,
                    detectedLanguage = "EN",
                    userQuery = "Hey Gandhi, open device Settings and check privacy status",
                    assistantResponse = "Namaste! Launching Android Settings right away. Savdhan aur surakshit rahein — your microphone stream was processed 100% offline on-device.",
                    executedAction = "App: Settings (com.android.settings)",
                    processingMode = "HYBRID_GEMINI",
                    latencyMs = 310L,
                    isPinned = false
                ),
                TranscriptEntity(
                    timestamp = now - 600_000L,
                    detectedLanguage = "HI",
                    userQuery = "तुम मेरी आवाज़ को कैसे सुरक्षित रखते हो?",
                    assistantResponse = "मैं आपकी आवाज़ को कभी भी इंटरनेट या क्लाउड पर नहीं भेजता। 'हे गांधी' वेक-वर्ड और स्पीच-टू-टेक्स्ट (Whisper/Vosk ONNX) पूरी तरह से आपके फ़ोन के अंदर ऑफ़लाइन चलते हैं, और ऑडियो तुरंत नष्ट कर दिया जाता है।",
                    executedAction = null,
                    processingMode = "STRICT_OFFLINE",
                    latencyMs = 82L,
                    isPinned = false
                )
            )
            sampleTranscripts.forEach { dao.insertTranscript(it) }
        }
    }

    suspend fun insertTranscript(transcript: TranscriptEntity): Long {
        return dao.insertTranscript(transcript)
    }

    suspend fun togglePinTranscript(transcript: TranscriptEntity) {
        dao.updateTranscript(transcript.copy(isPinned = !transcript.isPinned))
    }

    suspend fun deleteTranscript(id: Int) {
        dao.deleteTranscriptById(id)
    }

    suspend fun wipeAllTranscripts() {
        dao.wipeAllTranscripts()
    }

    suspend fun getTermuxScriptsSnapshot(): List<TermuxScriptEntity> {
        return dao.getTermuxScriptsSnapshot()
    }

    suspend fun saveTermuxScript(script: TermuxScriptEntity) {
        dao.insertTermuxScript(script)
    }

    suspend fun recordScriptExecution(script: TermuxScriptEntity) {
        dao.updateTermuxScript(
            script.copy(
                lastExecutedAt = System.currentTimeMillis(),
                executionCount = script.executionCount + 1
            )
        )
    }

    suspend fun deleteTermuxScript(id: Int) {
        dao.deleteTermuxScriptById(id)
    }

    fun exportTranscriptsToJson(transcripts: List<TranscriptEntity>): String {
        return prettyJson.encodeToString(transcripts)
    }

    fun exportTranscriptsToCsv(transcripts: List<TranscriptEntity>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val header = "ID,Timestamp,Language,Mode,UserQuery,AssistantResponse,ExecutedAction,LatencyMs\n"
        val rows = transcripts.joinToString("\n") { t ->
            val dateStr = sdf.format(Date(t.timestamp))
            val queryEsc = t.userQuery.replace("\"", "\"\"")
            val respEsc = t.assistantResponse.replace("\"", "\"\"")
            val actEsc = (t.executedAction ?: "None").replace("\"", "\"\"")
            "${t.id},\"$dateStr\",${t.detectedLanguage},${t.processingMode},\"$queryEsc\",\"$respEsc\",\"$actEsc\",${t.latencyMs}"
        }
        return header + rows
    }
}
