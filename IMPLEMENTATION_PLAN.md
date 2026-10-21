# Hey Gandhi (हे गांधी) — Implementation Plan & Module Reference

## 1. Architectural Overview & Privacy Guarantee

**Hey Gandhi** is a zero-login, privacy-first bilingual (Hindi & English) Android personal assistant designed around a **5-stage Offline-First Pipeline**:

1. **Stage 1 — Persistent Foreground Wake-Word Service (`service/GandhiWakeWordService.kt`)**
   - Runs as a persistent Android `Service` with `ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE`.
   - Captures 16kHz, 16-bit mono PCM audio frames via `AudioRecord` in a background coroutine (`512` samples per frame, matching Picovoice Porcupine / OpenWakeWord ONNX frame sizing).
   - Computes real-time RMS acoustic energy for Voice Activity Detection (VAD) and immediately zeroes out every PCM sample in RAM (`frameBuffer[i] = 0`) so raw voice biometrics are never persisted to disk or transmitted over the network.
   - `service/BootCompletedReceiver.kt` listens for `ACTION_BOOT_COMPLETED` to optionally re-arm the wake-word service when the device restarts.

2. **Stage 2 — Offline Bilingual Speech-to-Text (`domain/VoiceEngineManager.kt`)**
   - Upon detecting `"Hey Gandhi"` / `"हे गांधी"`, plays an acoustic confirmation chime (`ToneGenerator`) and starts Android's `SpeechRecognizer` configured with `RecognizerIntent.EXTRA_PREFER_OFFLINE = true`.
   - Defaults to Hindi-first (`hi-IN`) while supporting instant switching to Indian English (`en-IN`).

3. **Stage 3 — Hybrid / Offline-First Bilingual AI Router (`domain/GandhiAiProcessor.kt` & `data/remote/GeminiApiService.kt`)**
   - **Language Detection**: Inspects Unicode code points (`\u0900`..`\u097F` for Devanagari) and common spoken Hinglish vocabulary (`kholo`, `chalao`, `karo`, `batao`, etc.) to classify queries as `HI` or `EN`.
   - **Strict Offline Mode (`AiOperatingMode.STRICT_OFFLINE`)**: Executes 100% on-device with zero network traffic using local bilingual regex intent extraction and Hindi-first conversational responses.
   - **Hybrid Privacy Mode (`AiOperatingMode.HYBRID_GEMINI`)**: Strips all audio and sends only anonymized text to `v1beta/models/gemini-3.5-flash:generateContent` with Function Calling declarations (`launchApp` and `runTermuxScript`). Automatically falls back to the local engine if offline.

4. **Stage 4 — System & Termux Automation + Offline TTS (`domain/SystemTaskExecutor.kt`)**
   - **App Launching**: Uses `PackageManager.queryIntentActivities` (`ACTION_MAIN` / `CATEGORY_LAUNCHER`) and a bilingual alias dictionary (`सेटिंग्स` -> `settings`, `यूट्यूब` -> `youtube`, `व्हाट्सएप` -> `whatsapp`, `टर्मक्स` -> `termux`, `कैमरा` -> `camera`, `कैलकुलेटर` -> `calculator`) to launch installed apps with `FLAG_ACTIVITY_NEW_TASK`.
   - **Termux Python Automation**: Dispatches `com.termux.RUN_COMMAND` intents targeting `com.termux.app.RunCommandService` with `/data/data/com.termux/files/usr/bin/python` and `/data/data/com.termux/files/home/scripts/<script>.py`.
   - **Bilingual Offline TTS**: Inspects the response text for Devanagari characters and switches `TextToSpeech.language` between `Locale("hi", "IN")` and `Locale("en", "IN")`.

5. **Stage 5 — Local Room SQLite Vault (`data/local/*` & `data/repository/GandhiRepository.kt`)**
   - Stores all voice transcripts (`TranscriptEntity`) and custom Termux script mappings (`TermuxScriptEntity`) locally in `hey_gandhi_vault.db`.
   - Supports full-text search, language/action filtering, JSON/CSV local export, and a single-tap **Wipe All Local Data** panic action.

---

## 2. Project File Map

| File Path | Responsibility |
| :--- | :--- |
| `app/build.gradle.kts` | Configures `applicationId`, Room KSP, Kotlin Serialization, Retrofit, Secrets Gradle Plugin, and `noCompress` for `.onnx`, `.tflite`, `.ppn`, `.bin` models. |
| `app/src/main/AndroidManifest.xml` | Declares `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE`, `RECEIVE_BOOT_COMPLETED`, `com.termux.permission.RUN_COMMAND`, `<queries>` for launcher apps & Termux, and registers `GandhiWakeWordService` + `BootCompletedReceiver`. |
| `app/src/main/java/com/example/data/local/TranscriptEntity.kt` | Room `@Entity(tableName = "transcripts")` storing timestamp, language (`HI`/`EN`), user query, assistant response, executed action, processing mode, and latency. |
| `app/src/main/java/com/example/data/local/TermuxScriptEntity.kt` | Room `@Entity(tableName = "termux_scripts")` mapping Hindi & English voice phrases to `.py` scripts in `~/.termux/files/home/scripts/`. |
| `app/src/main/java/com/example/data/local/GandhiDao.kt` | Reactive `Flow` queries and `suspend` mutations for transcripts and Termux script aliases. |
| `app/src/main/java/com/example/data/local/GandhiDatabase.kt` | Singleton Room database (`hey_gandhi_vault.db`). |
| `app/src/main/java/com/example/data/repository/GandhiRepository.kt` | Repository managing local transcripts, Termux scripts, initial bilingual seed records, and JSON/CSV export formatting. |
| `app/src/main/java/com/example/data/remote/GeminiApiService.kt` | Retrofit interface & `@Serializable` DTOs for `gemini-3.5-flash` with Function Calling (`tools` & `functionCall`). |
| `app/src/main/java/com/example/domain/SystemTaskExecutor.kt` | Executes local `PackageManager` app launches and `com.termux.RUN_COMMAND` Python script intents. |
| `app/src/main/java/com/example/domain/GandhiAiProcessor.kt` | Hybrid/Offline-First Bilingual AI Router with Devanagari detection, Gemini tool calling, and local NLU fallback. |
| `app/src/main/java/com/example/domain/VoiceEngineManager.kt` | Manages offline `SpeechRecognizer` (`EXTRA_PREFER_OFFLINE`) and bilingual `TextToSpeech` (`hi-IN` / `en-IN`). |
| `app/src/main/java/com/example/service/GandhiWakeWordService.kt` | Persistent microphone Foreground Service reading 16kHz PCM audio frames and zeroing RAM buffers after RMS inspection. |
| `app/src/main/java/com/example/service/BootCompletedReceiver.kt` | Restores `GandhiWakeWordService` on device boot when enabled. |
| `app/src/main/java/com/example/ui/GandhiViewModel.kt` | Orchestrates the 5-stage pipeline, UI state flows, filters, exports, and service toggles. |
| `app/src/main/java/com/example/ui/components/AcousticOrbHeader.kt` | Custom Canvas 12-spoke Charkha acoustic waveform visualizer + service toggle + AI mode selector. |
| `app/src/main/java/com/example/ui/screens/AssistantHubScreen.kt` | Main voice hub with live STT trigger, 5-stage telemetry bar, response card, and bilingual quick triggers. |
| `app/src/main/java/com/example/ui/screens/TranscriptDashboardScreen.kt` | Local transcript vault with search, `HI`/`EN`/Action filters, JSON/CSV export dialog, and Wipe Vault panic button. |
| `app/src/main/java/com/example/ui/screens/AutomationHubScreen.kt` | Termux `.py` script mapper + live Installed Apps browser. |
| `app/src/main/java/com/example/ui/screens/ArchitectureBlueprintScreen.kt` | In-app interactive architectural reference & standalone APK packaging guide. |
| `app/src/main/java/com/example/MainActivity.kt` | Edge-to-edge activity with runtime permission handling and adaptive navigation (`NavigationBar` / `NavigationRail`). |
