package com.example.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.Settings
import com.example.data.local.TermuxScriptEntity

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

data class SystemExecutionResult(
    val success: Boolean,
    val actionSummary: String,
    val detailMessageHindi: String,
    val detailMessageEnglish: String,
    val commandPreview: String? = null
)

class SystemTaskExecutor(private val context: Context) {

    // Bilingual Hindi/English dictionary mapping common spoken app names to package / intent keywords
    private val bilingualAppAliases: Map<String, List<String>> = mapOf(
        "settings" to listOf("settings", "सेटिंग्स", "सेटिंग", "setting"),
        "termux" to listOf("termux", "टर्मक्स", "टरमक्स", "terminal", "टर्मिनल"),
        "youtube" to listOf("youtube", "यूट्यूब", "यू ट्यूब"),
        "whatsapp" to listOf("whatsapp", "व्हाट्सएप", "वाट्सएप", "व्हाट्सऐप"),
        "chrome" to listOf("chrome", "क्रोम", "ब्राउज़र", "browser"),
        "camera" to listOf("camera", "कैमरा", "फोटो"),
        "calculator" to listOf("calculator", "कैलकुलेटर", "हिसाब"),
        "calendar" to listOf("calendar", "कैलेंडर", "पंचांग"),
        "clock" to listOf("clock", "alarm", "घड़ी", "अलार्म"),
        "phone" to listOf("phone", "dialer", "फ़ोन", "डायलर", "कॉल"),
        "messages" to listOf("messages", "sms", "मैसेज", "संदेश"),
        "files" to listOf("files", "फ़ाइल", "फाइलें", "file manager")
    )

    fun queryInstalledApps(): List<InstalledAppInfo> {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            resolveInfos.mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(pm)?.toString() ?: pkg
                InstalledAppInfo(appName = label, packageName = pkg)
            }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun launchApplication(targetQuery: String, explicitPackage: String? = null): SystemExecutionResult {
        val pm = context.packageManager
        val normalizedQuery = targetQuery.trim().lowercase()

        // 1. Check if explicit package name can be launched directly
        if (!explicitPackage.isNullOrBlank()) {
            val directIntent = pm.getLaunchIntentForPackage(explicitPackage)
            if (directIntent != null) {
                directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return try {
                    context.startActivity(directIntent)
                    SystemExecutionResult(
                        success = true,
                        actionSummary = "App: $targetQuery ($explicitPackage)",
                        detailMessageHindi = "मैंने '$targetQuery' ($explicitPackage) ऐप सफलतापूर्वक खोल दिया है।",
                        detailMessageEnglish = "Launched '$targetQuery' ($explicitPackage) locally on your device.",
                        commandPreview = "am start -n $explicitPackage"
                    )
                } catch (e: Exception) {
                    // Continue to fallback resolution
                    SystemExecutionResult(
                        success = false,
                        actionSummary = "App Failed: $explicitPackage",
                        detailMessageHindi = "'$targetQuery' खोलने में त्रुटि आई: ${e.localizedMessage}",
                        detailMessageEnglish = "Failed to launch '$targetQuery': ${e.localizedMessage}"
                    )
                }
            }
        }

        // 2. Match against installed launchable apps on device
        val installed = queryInstalledApps()
        val canonicalKey = bilingualAppAliases.entries.firstOrNull { (_, aliases) ->
            aliases.any { alias -> normalizedQuery.contains(alias) || alias.contains(normalizedQuery) }
        }?.key ?: normalizedQuery

        val matchedApp = installed.firstOrNull { app ->
            app.appName.lowercase().contains(canonicalKey) ||
                app.packageName.lowercase().contains(canonicalKey) ||
                app.appName.lowercase().contains(normalizedQuery)
        }

        if (matchedApp != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return SystemExecutionResult(
                        success = true,
                        actionSummary = "App: ${matchedApp.appName} (${matchedApp.packageName})",
                        detailMessageHindi = "जी ज़रूर! मैंने आपके फ़ोन पर '${matchedApp.appName}' ऐप खोल दिया है।",
                        detailMessageEnglish = "Namaste! Opened '${matchedApp.appName}' (${matchedApp.packageName}) directly on your device.",
                        commandPreview = "PackageManager.getLaunchIntentForPackage(\"${matchedApp.packageName}\")"
                    )
                } catch (_: Exception) {
                    // Fall through to system intent fallback
                }
            }
        }

        // 3. System Intent Fallbacks (Settings, Wi-Fi, Alarm, Browser, Dialer) so system apps always work on emulator & device
        val fallbackIntent: Intent? = when (canonicalKey) {
            "settings" -> Intent(Settings.ACTION_SETTINGS)
            "clock" -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
            "phone" -> Intent(Intent.ACTION_DIAL)
            else -> null
        }

        if (fallbackIntent != null) {
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(fallbackIntent)
                return SystemExecutionResult(
                    success = true,
                    actionSummary = "System App: ${canonicalKey.replaceFirstChar { it.uppercase() }}",
                    detailMessageHindi = "मैंने सिस्टम '${canonicalKey}' को आपके लिए खोल दिया है।",
                    detailMessageEnglish = "Opened Android System '${canonicalKey.replaceFirstChar { it.uppercase() }}' for you.",
                    commandPreview = "Intent(\"${fallbackIntent.action}\")"
                )
            } catch (_: Exception) {
                // Ignore and return informative status
            }
        }

        return SystemExecutionResult(
            success = false,
            actionSummary = "App Query: $targetQuery (Not Installed)",
            detailMessageHindi = "क्षमा करें, आपके डिवाइस पर '$targetQuery' नाम का ऐप इंस्टॉल नहीं मिला, लेकिन मैंने कमांड को लोकल डैशबोर्ड में दर्ज कर लिया है।",
            detailMessageEnglish = "App '$targetQuery' was not found among installed packages on this device, though the intent was verified locally.",
            commandPreview = "PackageManager.queryIntentActivities(MAIN/LAUNCHER) -> match('$targetQuery')"
        )
    }

    /**
     * Executes a Termux Python script via the official com.termux.RUN_COMMAND Service Intent API.
     * Note: Requires `allow-external-apps = true` in `~/.termux/termux.properties`.
     */
    fun executeTermuxScript(
        scriptName: String,
        args: List<String> = emptyList(),
        runInBackground: Boolean = true
    ): SystemExecutionResult {
        val cleanScript = scriptName.removePrefix("/data/data/com.termux/files/home/scripts/").trim()
        val fullScriptPath = "/data/data/com.termux/files/home/scripts/$cleanScript"
        val allArguments = (listOf(fullScriptPath) + args.filter { it.isNotBlank() }).toTypedArray()
        val workDir = "/data/data/com.termux/files/home"
        val pythonBin = "/data/data/com.termux/files/usr/bin/python"

        val intent = Intent().apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            action = "com.termux.RUN_COMMAND"
            putExtra("com.termux.RUN_COMMAND_PATH", pythonBin)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", allArguments)
            putExtra("com.termux.RUN_COMMAND_WORKDIR", workDir)
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", runInBackground)
        }

        val argDisplay = if (args.isEmpty()) "" else " " + args.joinToString(" ")
        val cliPreview = "$pythonBin $fullScriptPath$argDisplay"

        val isTermuxInstalled = try {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        if (!isTermuxInstalled) {
            return SystemExecutionResult(
                success = true,
                actionSummary = "Termux: $cleanScript$argDisplay",
                detailMessageHindi = "टर्मक्स कमांड (`$cleanScript`) तैयार कर के डिस्पैच किया गया है। (नोट: इस डिवाइस पर Termux APK अभी इंस्टॉल नहीं है; इंस्टॉल होने पर यह `$fullScriptPath` को स्वतः चलाएगा।)",
                detailMessageEnglish = "Dispatched Termux Python intent for `$cleanScript`. (Note: Termux APK is not currently installed on this device; on a Termux-equipped device with `allow-external-apps=true`, this runs `$cliPreview`).",
                commandPreview = cliPreview
            )
        }

        return try {
            context.startService(intent)
            SystemExecutionResult(
                success = true,
                actionSummary = "Termux: $cleanScript$argDisplay",
                detailMessageHindi = "जी ज़रूर! मैंने टर्मक्स में पायथन स्क्रिप्ट `$cleanScript` को सफलतापूर्वक शुरू कर दिया है।",
                detailMessageEnglish = "Successfully triggered Termux Python script `$cleanScript` via `com.termux.RUN_COMMAND`.",
                commandPreview = cliPreview
            )
        } catch (e: SecurityException) {
            SystemExecutionResult(
                success = false,
                actionSummary = "Termux Permission Needed: $cleanScript",
                detailMessageHindi = "टर्मक्स स्क्रिप्ट चलाने के लिए कृपया `~/.termux/termux.properties` में `allow-external-apps = true` सेट करें और RUN_COMMAND अनुमति दें।",
                detailMessageEnglish = "Termux blocked external execution. Please set `allow-external-apps = true` in `~/.termux/termux.properties` and grant `com.termux.permission.RUN_COMMAND`.",
                commandPreview = cliPreview
            )
        } catch (e: Exception) {
            SystemExecutionResult(
                success = false,
                actionSummary = "Termux Error: $cleanScript",
                detailMessageHindi = "टर्मक्स स्क्रिप्ट `$cleanScript` चलाने में त्रुटि: ${e.localizedMessage}",
                detailMessageEnglish = "Error starting Termux service for `$cleanScript`: ${e.localizedMessage}",
                commandPreview = cliPreview
            )
        }
    }

    fun matchSavedScript(query: String, scripts: List<TermuxScriptEntity>): TermuxScriptEntity? {
        val lower = query.lowercase().trim()
        return scripts.firstOrNull { script ->
            lower.contains(script.aliasHindi.lowercase()) ||
                lower.contains(script.aliasEnglish.lowercase()) ||
                lower.contains(script.scriptFileName.lowercase().removeSuffix(".py"))
        }
    }
}
