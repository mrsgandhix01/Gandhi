package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.GandhiWakeWordService
import com.example.ui.GandhiTab
import com.example.ui.GandhiViewModel
import com.example.ui.screens.ArchitectureBlueprintScreen
import com.example.ui.screens.AssistantHubScreen
import com.example.ui.screens.AutomationHubScreen
import com.example.ui.screens.TranscriptDashboardScreen
import com.example.ui.theme.AshokaEmerald
import com.example.ui.theme.KhadiIvory
import com.example.ui.theme.KhadiMuted
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SwarajSaffron

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HeyGandhiApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeyGandhiApp(
    viewModel: GandhiViewModel = viewModel()
) {
    val context = LocalContext.current

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isWakeServiceRunning.collectAsStateWithLifecycle()
    val liveRmsLevel by viewModel.liveRmsLevel.collectAsStateWithLifecycle()
    val framesProcessed by viewModel.framesProcessedCount.collectAsStateWithLifecycle()
    val wakeDetections by viewModel.wakeWordDetections.collectAsStateWithLifecycle()
    val sttState by viewModel.sttState.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val preferredSttLocale by viewModel.preferredSttLocale.collectAsStateWithLifecycle()
    val ttsActiveLanguage by viewModel.ttsActiveLanguage.collectAsStateWithLifecycle()
    val aiMode by viewModel.aiMode.collectAsStateWithLifecycle()
    val pipelineStage by viewModel.pipelineStage.collectAsStateWithLifecycle()
    val latestTurnResult by viewModel.latestTurnResult.collectAsStateWithLifecycle()
    val filteredTranscripts by viewModel.filteredTranscripts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val languageFilter by viewModel.languageFilter.collectAsStateWithLifecycle()
    val exportedPreview by viewModel.exportedDataPreview.collectAsStateWithLifecycle()
    val termuxScripts by viewModel.termuxScripts.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val autoStartOnBoot by viewModel.autoStartOnBoot.collectAsStateWithLifecycle()
    val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    // BackHandler on secondary tabs to return to ASSISTANT home tab
    if (currentTab != GandhiTab.ASSISTANT) {
        BackHandler {
            viewModel.selectTab(GandhiTab.ASSISTANT)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (micGranted) {
            GandhiWakeWordService.startService(context)
        }
        viewModel.triggerLiveVoiceWakeWord()
    }

    val requestMicAndTriggerWake = {
        val permsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permsToRequest.toTypedArray())
        } else {
            viewModel.triggerLiveVoiceWakeWord()
        }
    }

    val navItems = listOf(
        Triple(GandhiTab.ASSISTANT, "Assistant", Icons.Filled.Mic to Icons.Outlined.Mic),
        Triple(GandhiTab.TRANSCRIPTS, "Vault (${filteredTranscripts.size})", Icons.Filled.History to Icons.Outlined.History),
        Triple(GandhiTab.AUTOMATION, "Termux/Apps", Icons.Filled.Terminal to Icons.Outlined.Terminal),
        Triple(GandhiTab.BLUEPRINT, "Blueprint", Icons.Filled.Architecture to Icons.Outlined.Architecture)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceRunning) AshokaEmerald else SwarajSaffron)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Hey Gandhi • हे गांधी",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = KhadiIvory
                                )
                                Text(
                                    text = "100% Offline Audio • Bilingual Hindi & English Assistant",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KhadiMuted
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ObsidianSurface
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        tonalElevation = 8.dp
                    ) {
                        navItems.forEach { (tab, label, icons) ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ObsidianBg,
                                    selectedTextColor = SwarajSaffron,
                                    indicatorColor = SwarajSaffron,
                                    unselectedIconColor = KhadiMuted,
                                    unselectedTextColor = KhadiMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { (tab, label, icons) ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) icons.first else icons.second,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    AnimatedVisibility(visible = statusBannerMessage != null) {
                        statusBannerMessage?.let { msg ->
                            Surface(
                                color = ObsidianSurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SwarajSaffron,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.dismissBanner() }) {
                                        Text("OK", color = KhadiIvory)
                                    }
                                }
                            }
                        }
                    }

                    when (currentTab) {
                        GandhiTab.ASSISTANT -> AssistantHubScreen(
                            isServiceRunning = isServiceRunning,
                            liveRmsLevel = liveRmsLevel,
                            framesProcessed = framesProcessed,
                            wakeDetections = wakeDetections,
                            sttState = sttState,
                            partialTranscript = partialTranscript,
                            preferredSttLocale = preferredSttLocale,
                            ttsActiveLanguage = ttsActiveLanguage,
                            aiMode = aiMode,
                            isGeminiKeyConfigured = viewModel.isGeminiKeyConfigured,
                            pipelineStage = pipelineStage,
                            latestTurnResult = latestTurnResult,
                            onToggleService = { viewModel.toggleWakeWordService() },
                            onSelectAiMode = { viewModel.setAiOperatingMode(it) },
                            onSetSttLocale = { viewModel.setPreferredSttLanguage(it) },
                            onRequestMicAndTriggerWake = requestMicAndTriggerWake,
                            onExecuteCommand = { cmd -> viewModel.executeVoiceOrTextCommand(cmd, true) },
                            onSpeakAgain = { text -> viewModel.speakTextAgain(text) }
                        )

                        GandhiTab.TRANSCRIPTS -> TranscriptDashboardScreen(
                            transcripts = filteredTranscripts,
                            searchQuery = searchQuery,
                            languageFilter = languageFilter,
                            exportedPreview = exportedPreview,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            onLanguageFilterChange = { viewModel.updateLanguageFilter(it) },
                            onTogglePin = { viewModel.togglePinTranscript(it) },
                            onDeleteTranscript = { viewModel.deleteTranscript(it) },
                            onWipeAllData = { viewModel.wipeAllLocalData() },
                            onExportFormat = { viewModel.showExportDialog(it) },
                            onDismissExport = { viewModel.dismissExportDialog() },
                            onSpeakResponse = { viewModel.speakTextAgain(it) }
                        )

                        GandhiTab.AUTOMATION -> AutomationHubScreen(
                            termuxScripts = termuxScripts,
                            installedApps = installedApps,
                            autoStartOnBoot = autoStartOnBoot,
                            onToggleAutoStartOnBoot = { viewModel.setAutoStartOnBoot(it) },
                            onRunScript = { script ->
                                viewModel.executeSavedTermuxScript(script)
                                viewModel.selectTab(GandhiTab.ASSISTANT)
                            },
                            onAddScript = { hi, en, file, args, desc, bg ->
                                viewModel.addCustomTermuxScript(hi, en, file, args, desc, bg)
                            },
                            onDeleteScript = { viewModel.deleteTermuxScript(it) },
                            onLaunchInstalledApp = { viewModel.launchInstalledAppDirectly(it) }
                        )

                        GandhiTab.BLUEPRINT -> ArchitectureBlueprintScreen()
                    }
                }
            }
        }
    }
}
