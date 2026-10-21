package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.TermuxScriptEntity
import com.example.domain.InstalledAppInfo
import com.example.ui.theme.AshokaEmerald
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.KhadiIvory
import com.example.ui.theme.KhadiMuted
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SwarajSaffron

@Composable
fun AutomationHubScreen(
    termuxScripts: List<TermuxScriptEntity>,
    installedApps: List<InstalledAppInfo>,
    autoStartOnBoot: Boolean,
    onToggleAutoStartOnBoot: (Boolean) -> Unit,
    onRunScript: (TermuxScriptEntity) -> Unit,
    onAddScript: (String, String, String, String, String, Boolean) -> Unit,
    onDeleteScript: (Int) -> Unit,
    onLaunchInstalledApp: (InstalledAppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by rememberSaveable { mutableStateOf(0) } // 0 = Termux Python Scripts, 1 = Installed Apps
    var showAddScriptDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Section Switcher + Add Script Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    label = { Text("Termux .py Scripts (${termuxScripts.size})") },
                    leadingIcon = {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SwarajSaffron.copy(alpha = 0.22f),
                        selectedLabelColor = SwarajSaffron,
                        selectedLeadingIconColor = SwarajSaffron
                    ),
                    modifier = Modifier.testTag("section_termux_scripts_chip")
                )

                FilterChip(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    label = { Text("Device Apps (${installedApps.size})") },
                    leadingIcon = {
                        Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChakraCyan.copy(alpha = 0.22f),
                        selectedLabelColor = ChakraCyan,
                        selectedLeadingIconColor = ChakraCyan
                    ),
                    modifier = Modifier.testTag("section_device_apps_chip")
                )
            }

            if (selectedSection == 0) {
                Button(
                    onClick = { showAddScriptDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SwarajSaffron,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_termux_script_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Script", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Map .py", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedSection == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Start \"Hey Gandhi\" on Device Boot",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = KhadiIvory
                                    )
                                    Text(
                                        text = "RECEIVE_BOOT_COMPLETED auto-arms offline wake-word service",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KhadiMuted
                                    )
                                }
                                Switch(
                                    checked = autoStartOnBoot,
                                    onCheckedChange = onToggleAutoStartOnBoot
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ObsidianBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Termux Config Prerequisite:\necho \"allow-external-apps = true\" >> ~/.termux/termux.properties",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AshokaEmerald,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                items(termuxScripts, key = { it.id }) { script ->
                    TermuxScriptCard(
                        script = script,
                        onRun = { onRunScript(script) },
                        onDelete = { onDeleteScript(script.id) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(installedApps, key = { it.packageName }) { app ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = KhadiIvory
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KhadiMuted
                                )
                            }
                            Button(
                                onClick = { onLaunchInstalledApp(app) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChakraCyan.copy(alpha = 0.2f),
                                    contentColor = ChakraCyan
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Launch,
                                    contentDescription = "Launch ${app.appName}",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Launch")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddScriptDialog) {
        var aliasHi by remember { mutableStateOf("") }
        var aliasEn by remember { mutableStateOf("") }
        var scriptFile by remember { mutableStateOf("") }
        var scriptArgs by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddScriptDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text("Map Termux Python Script", color = KhadiIvory, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = aliasHi,
                        onValueChange = { aliasHi = it },
                        label = { Text("Hindi Voice Trigger (e.g. फ़ोटो सिंक करो)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = aliasEn,
                        onValueChange = { aliasEn = it },
                        label = { Text("English Voice Trigger (e.g. sync photos)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = scriptFile,
                        onValueChange = { scriptFile = it },
                        label = { Text("Script Name in ~/scripts/ (e.g. sync_photos.py)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = scriptArgs,
                        onValueChange = { scriptArgs = it },
                        label = { Text("CLI Arguments (e.g. --compress --local)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (scriptFile.isNotBlank()) {
                            onAddScript(aliasHi, aliasEn, scriptFile, scriptArgs, desc, true)
                            showAddScriptDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SwarajSaffron, contentColor = ObsidianBg)
                ) {
                    Text("Save Mapping", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddScriptDialog = false }) {
                    Text("Cancel", color = KhadiMuted)
                }
            }
        )
    }
}

@Composable
private fun TermuxScriptCard(
    script: TermuxScriptEntity,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
            .testTag("termux_script_card_${script.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = SwarajSaffron,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = script.scriptFileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = KhadiIvory,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Runs: ${script.executionCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AshokaEmerald
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove script mapping",
                            tint = DangerCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = script.description,
                style = MaterialTheme.typography.bodyMedium,
                color = KhadiMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ObsidianBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "हिंदी ट्रिगर: \"हे गांधी, ${script.aliasHindi}\"",
                        style = MaterialTheme.typography.labelLarge,
                        color = SwarajSaffron
                    )
                    Text(
                        text = "English Trigger: \"Hey Gandhi, ${script.aliasEnglish}\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChakraCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "/data/data/com.termux/files/usr/bin/python ~/scripts/${script.scriptFileName} ${script.defaultArgs}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AshokaEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onRun,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AshokaEmerald.copy(alpha = 0.2f),
                    contentColor = AshokaEmerald
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("run_script_button_${script.id}")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Execute in Termux", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Execute via com.termux.RUN_COMMAND", fontWeight = FontWeight.Bold)
            }
        }
    }
}
