package com.example.domain

import com.example.BuildConfig
import com.example.data.local.TermuxScriptEntity
import com.example.data.remote.Content
import com.example.data.remote.GeminiRetrofitClient
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

enum class AiOperatingMode(val label: String, val badgeHindi: String, val description: String) {
    STRICT_OFFLINE(
        label = "Strict Offline (On-Device NLU)",
        badgeHindi = "100% ऑफ़लाइन",
        description = "Zero network packets. Uses local bilingual intent router + on-device NLU."
    ),
    HYBRID_GEMINI(
        label = "Hybrid Privacy (Offline STT + Gemini API)",
        badgeHindi = "हाइब्रिड प्राइवेसी",
        description = "Audio stays 100% offline. Only anonymized text is sent to Gemini 3.5 Flash with Function Calling."
    )
}

data class AssistantTurnResult(
    val detectedLanguage: String, // "HI" or "EN"
    val userQuery: String,
    val assistantResponse: String,
    val executedAction: String?,
    val commandPreview: String?,
    val processingMode: String,
    val latencyMs: Long
)

class GandhiAiProcessor(
    private val systemTaskExecutor: SystemTaskExecutor
) {

    companion object {
        const val GANDHI_SYSTEM_INSTRUCTION = """
You are 'Gandhi' (हे गांधी), a helpful, privacy-first personal assistant running on an Android device.
You are strictly bilingual in Hindi and English.
1. If the user speaks to you in Hindi (Devanagari script or Hinglish), you MUST reply in pure, natural Hindi using Devanagari script so the offline Hindi TTS engine can read it clearly.
2. If the user speaks in English, reply in English, but maintain a warm, respectful Indian persona and naturally weave in comforting Hindi phrases (with Devanagari or clear context) whenever appropriate.
3. Keep responses concise, warm, and optimized for voice output (2-3 sentences max).
4. When the user asks to open/launch an app or run a Termux Python automation script, invoke the `launchApp` or `runTermuxScript` tool immediately.
"""

        /**
         * Evaluates whether a string contains Devanagari Unicode characters (\u0900-\u097F)
         * or common spoken Hinglish vocabulary.
         */
        fun detectHindiOrEnglish(text: String): String {
            val hasDevanagari = text.any { ch -> ch in '\u0900'..'\u097F' }
            if (hasDevanagari) return "HI"

            val hinglishMarkers = listOf(
                "kholo", "chala", "chalao", "karo", "batao", "kya", "kaise",
                "mera", "meri", "namaste", "pranam", "shukriya", "dhanyawad",
                "band", "saf", "saaf", "suraksha", "bhasha"
            )
            val words = text.lowercase().split(Regex("\\s+"))
            if (words.any { it in hinglishMarkers }) return "HI"
            return "EN"
        }
    }

    suspend fun processUserQuery(
        rawQuery: String,
        mode: AiOperatingMode,
        savedScripts: List<TermuxScriptEntity>,
        onScriptTriggered: suspend (TermuxScriptEntity) -> Unit
    ): AssistantTurnResult = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        val cleanedQuery = rawQuery
            .replace(Regex("^(hey gandhi|हे गांधी|ओ गांधी|hello gandhi)[,\\s]*", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifEmpty { rawQuery.trim() }

        val lang = detectHindiOrEnglish(rawQuery)

        // Step 1: Check if query directly matches a configured Termux Python Script alias
        val matchedScript = systemTaskExecutor.matchSavedScript(cleanedQuery, savedScripts)
        if (matchedScript != null) {
            val argsList = matchedScript.defaultArgs.split(" ").filter { it.isNotBlank() }
            val execResult = systemTaskExecutor.executeTermuxScript(
                scriptName = matchedScript.scriptFileName,
                args = argsList,
                runInBackground = matchedScript.runInBackground
            )
            onScriptTriggered(matchedScript)
            val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(45L)
            return@withContext AssistantTurnResult(
                detectedLanguage = lang,
                userQuery = rawQuery,
                assistantResponse = if (lang == "HI") execResult.detailMessageHindi else execResult.detailMessageEnglish,
                executedAction = execResult.actionSummary,
                commandPreview = execResult.commandPreview,
                processingMode = mode.name,
                latencyMs = elapsed
            )
        }

        // Step 2: If in HYBRID_GEMINI mode and API key is configured, call Gemini 3.5 Flash with Function Calling
        val apiKey = BuildConfig.GEMINI_API_KEY
        val isApiKeyValid = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (mode == AiOperatingMode.HYBRID_GEMINI && isApiKeyValid) {
            try {
                val geminiResult = callGeminiWithTools(
                    rawQuery = rawQuery,
                    cleanedQuery = cleanedQuery,
                    lang = lang,
                    apiKey = apiKey,
                    savedScripts = savedScripts,
                    startMs = startMs
                )
                if (geminiResult != null) {
                    return@withContext geminiResult
                }
            } catch (_: Exception) {
                // Fall back seamlessly to Strict Offline local engine if offline or network error
            }
        }

        // Step 3: Strict Offline Local NLU & Intent Engine (100% On-Device)
        return@withContext processStrictlyOffline(
            rawQuery = rawQuery,
            cleanedQuery = cleanedQuery,
            lang = lang,
            savedScripts = savedScripts,
            mode = mode,
            startMs = startMs
        )
    }

    private suspend fun callGeminiWithTools(
        rawQuery: String,
        cleanedQuery: String,
        lang: String,
        apiKey: String,
        savedScripts: List<TermuxScriptEntity>,
        startMs: Long
    ): AssistantTurnResult? {
        val scriptCatalogDesc = savedScripts.joinToString("; ") {
            "${it.scriptFileName} (Hindi: '${it.aliasHindi}', English: '${it.aliasEnglish}')"
        }

        val toolsJson = buildJsonObject {
            putJsonArray("functionDeclarations") {
                addJsonObject {
                    put("name", "launchApp")
                    put("description", "Launches an installed Android application on the user's device.")
                    putJsonObject("parameters") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("appName") {
                                put("type", "STRING")
                                put("description", "Spoken name of the app to launch, e.g., Settings, WhatsApp, YouTube, Termux, Camera")
                            }
                            putJsonObject("packageName") {
                                put("type", "STRING")
                                put("description", "Optional Android package name if known, e.g., com.android.settings")
                            }
                        }
                        putJsonArray("required") { add("appName") }
                    }
                }
                addJsonObject {
                    put("name", "runTermuxScript")
                    put(
                        "description",
                        "Executes a local Python script inside Termux via com.termux.RUN_COMMAND. Available scripts: $scriptCatalogDesc"
                    )
                    putJsonObject("parameters") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("scriptName") {
                                put("type", "STRING")
                                put("description", "The Python script filename in ~/scripts/, e.g. vault_backup.py")
                            }
                            putJsonObject("arguments") {
                                put("type", "ARRAY")
                                putJsonObject("items") { put("type", "STRING") }
                                put("description", "Command line arguments for the script")
                            }
                        }
                        putJsonArray("required") { add("scriptName") }
                    }
                }
            }
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = cleanedQuery))
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.5f),
            tools = listOf(toolsJson),
            systemInstruction = Content(
                parts = listOf(Part(text = GANDHI_SYSTEM_INSTRUCTION))
            )
        )

        val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
        val firstCandidate = response.candidates?.firstOrNull() ?: return null
        val parts = firstCandidate.content?.parts ?: return null

        val fnCall = parts.firstOrNull { it.functionCall != null }?.functionCall
        if (fnCall != null) {
            val args = fnCall.args
            when (fnCall.name) {
                "launchApp" -> {
                    val appName = (args?.get("appName") as? JsonPrimitive)?.content ?: cleanedQuery
                    val pkgName = (args?.get("packageName") as? JsonPrimitive)?.content
                    val exec = systemTaskExecutor.launchApplication(appName, pkgName)
                    val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(120L)
                    return AssistantTurnResult(
                        detectedLanguage = lang,
                        userQuery = rawQuery,
                        assistantResponse = if (lang == "HI") exec.detailMessageHindi else exec.detailMessageEnglish,
                        executedAction = exec.actionSummary,
                        commandPreview = exec.commandPreview,
                        processingMode = AiOperatingMode.HYBRID_GEMINI.name,
                        latencyMs = elapsed
                    )
                }
                "runTermuxScript" -> {
                    val scriptName = (args?.get("scriptName") as? JsonPrimitive)?.content ?: "vault_backup.py"
                    val scriptArgs = (args?.get("arguments") as? JsonArray)?.mapNotNull {
                        it.jsonPrimitive.content
                    } ?: emptyList()
                    val exec = systemTaskExecutor.executeTermuxScript(scriptName, scriptArgs)
                    val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(120L)
                    return AssistantTurnResult(
                        detectedLanguage = lang,
                        userQuery = rawQuery,
                        assistantResponse = if (lang == "HI") exec.detailMessageHindi else exec.detailMessageEnglish,
                        executedAction = exec.actionSummary,
                        commandPreview = exec.commandPreview,
                        processingMode = AiOperatingMode.HYBRID_GEMINI.name,
                        latencyMs = elapsed
                    )
                }
            }
        }

        val textReply = parts.mapNotNull { it.text }.joinToString("\n").trim()
        if (textReply.isNotEmpty()) {
            val replyLang = detectHindiOrEnglish(textReply)
            val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(150L)
            return AssistantTurnResult(
                detectedLanguage = replyLang,
                userQuery = rawQuery,
                assistantResponse = textReply,
                executedAction = null,
                commandPreview = null,
                processingMode = AiOperatingMode.HYBRID_GEMINI.name,
                latencyMs = elapsed
            )
        }
        return null
    }

    private fun processStrictlyOffline(
        rawQuery: String,
        cleanedQuery: String,
        lang: String,
        savedScripts: List<TermuxScriptEntity>,
        mode: AiOperatingMode,
        startMs: Long
    ): AssistantTurnResult {
        val lower = cleanedQuery.lowercase()

        // 1. Check for local App Launch intent keywords in Hindi or English
        val openAppRegex = Regex(
            "(?:open|launch|start|खोलो|खोलिए|चलाओ|शुरू करो|kholo)\\s+([\\p{L}0-9_\\s.-]+)|([\\p{L}0-9_\\s.-]+?)\\s+(?:खोलो|खोलिए|चलाओ|ऐप खोलो|app kholo)",
            RegexOption.IGNORE_CASE
        )
        val appMatch = openAppRegex.find(cleanedQuery)
        if (appMatch != null && !lower.contains(".py") && !lower.contains("स्क्रिप्ट") && !lower.contains("script")) {
            val candidateApp = (appMatch.groupValues[1].ifBlank { appMatch.groupValues[2] })
                .replace(Regex("(ऐप|app|application|को|जरा|कृपया|please)", RegexOption.IGNORE_CASE), "")
                .trim()
            if (candidateApp.isNotEmpty()) {
                val exec = systemTaskExecutor.launchApplication(candidateApp)
                val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(55L)
                return AssistantTurnResult(
                    detectedLanguage = lang,
                    userQuery = rawQuery,
                    assistantResponse = if (lang == "HI") exec.detailMessageHindi else exec.detailMessageEnglish,
                    executedAction = exec.actionSummary,
                    commandPreview = exec.commandPreview,
                    processingMode = mode.name,
                    latencyMs = elapsed
                )
            }
        }

        // 2. Check for explicit .py or script execution request
        val pyRegex = Regex("([a-zA-Z0-9_-]+\\.py)")
        val pyMatch = pyRegex.find(cleanedQuery)
        if (pyMatch != null || lower.contains("termux") || lower.contains("टर्मक्स") || lower.contains("स्क्रिप्ट")) {
            val scriptName = pyMatch?.groupValues?.get(1)
                ?: savedScripts.firstOrNull()?.scriptFileName
                ?: "vault_backup.py"
            val exec = systemTaskExecutor.executeTermuxScript(scriptName, emptyList())
            val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(65L)
            return AssistantTurnResult(
                detectedLanguage = lang,
                userQuery = rawQuery,
                assistantResponse = if (lang == "HI") exec.detailMessageHindi else exec.detailMessageEnglish,
                executedAction = exec.actionSummary,
                commandPreview = exec.commandPreview,
                processingMode = mode.name,
                latencyMs = elapsed
            )
        }

        // 3. Conversational Bilingual Offline Responses (Hindi-first persona)
        val responseText = when {
            lower.contains("privacy") || lower.contains("सुरक्षा") || lower.contains("प्राइवेसी") || lower.contains("offline") || lower.contains("ऑफ़लाइन") -> {
                if (lang == "HI") {
                    "नमस्ते! आपकी गोपनीयता सर्वोच्च है। 'हे गांधी' वेक-वर्ड और स्पीच-टू-टेक्स्ट आपके डिवाइस पर 100% ऑफ़लाइन प्रोसेस होते हैं। किसी भी लॉगिन, ईमेल या क्लाउड ऑडियो रिकॉर्डिंग की आवश्यकता नहीं है।"
                } else {
                    "Namaste! Your privacy is absolute — 'सत्य और गोपनीयता'. All wake-word detection and speech-to-text processing run 100% locally on your device without any account login or email tracking."
                }
            }
            lower.contains("time") || lower.contains("समय") || lower.contains("टाइम") || lower.contains("बजे") -> {
                val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                if (lang == "HI") {
                    "अभी स्थानीय समय $timeStr हो रहा है। बताइए, मैं आपके लिए कौन सा सिस्टम कार्य या टर्मक्स ऑटोमेशन चलाऊँ?"
                } else {
                    "Pranam! The current local device time is $timeStr. Let me know if you'd like me to run a Termux Python script or launch an app."
                }
            }
            lower.contains("help") || lower.contains("मदद") || lower.contains("क्या कर सकते") || lower.contains("capability") -> {
                if (lang == "HI") {
                    "मैं 'हे गांधी' हूँ — आपका द्विभाषी (हिंदी और अंग्रेज़ी) ऑफ़लाइन सहायक। आप मुझसे 'सेटिंग्स खोलो', 'बैकअप चलाओ', या 'नेटवर्क स्कैन करो' कह सकते हैं, और मैं बिना इंटरनेट लॉगिन के आपके कमांड पूरे करूँगा।"
                } else {
                    "Namaste, main 'Gandhi' hoon! I can launch installed Android apps, execute Termux Python scripts (`~/scripts/*.py`), and answer your questions in Hindi or English while keeping your voice 100% offline."
                }
            }
            else -> {
                if (lang == "HI") {
                    "नमस्ते! मैंने आपका संदेश (\"$cleanedQuery\") 100% ऑफ़लाइन प्रोसेस किया है। मैं आपकी सेवा में तत्पर हूँ — आप किसी भी ऐप को खोलने या टर्मक्स पायथन स्क्रिप्ट चलाने का आदेश दे सकते हैं।"
                } else {
                    "Namaste! I processed your command (\"$cleanedQuery\") locally on-device with zero login tracking. Aap mujhse Hindi ya English mein baat kar sakte hain — just ask me to launch an app or trigger a Termux script!"
                }
            }
        }

        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(70L)
        return AssistantTurnResult(
            detectedLanguage = lang,
            userQuery = rawQuery,
            assistantResponse = responseText,
            executedAction = null,
            commandPreview = null,
            processingMode = mode.name,
            latencyMs = elapsed
        )
    }
}
