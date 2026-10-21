package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.TranscriptEntity
import com.example.ui.LanguageFilter
import com.example.ui.theme.AshokaEmerald
import com.example.ui.theme.ChakraCyan
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.KhadiIvory
import com.example.ui.theme.KhadiMuted
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SwarajSaffron
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TranscriptDashboardScreen(
    transcripts: List<TranscriptEntity>,
    searchQuery: String,
    languageFilter: LanguageFilter,
    exportedPreview: Pair<String, String>?,
    onSearchQueryChange: (String) -> Unit,
    onLanguageFilterChange: (LanguageFilter) -> Unit,
    onTogglePin: (TranscriptEntity) -> Unit,
    onDeleteTranscript: (Int) -> Unit,
    onWipeAllData: () -> Unit,
    onExportFormat: (String) -> Unit,
    onDismissExport: () -> Unit,
    onSpeakResponse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmWipeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header + Export & Wipe Panic Actions
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Local Room Vault",
                            tint = AshokaEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Local Transcript Vault",
                                style = MaterialTheme.typography.titleLarge,
                                color = KhadiIvory
                            )
                            Text(
                                text = "${transcripts.size} records • Zero Cloud Sync • On-Device SQLite",
                                style = MaterialTheme.typography.labelSmall,
                                color = AshokaEmerald
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onExportFormat("JSON") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export JSON",
                            tint = SwarajSaffron,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("JSON", color = KhadiIvory)
                    }

                    OutlinedButton(
                        onClick = { onExportFormat("CSV") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = ChakraCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV", color = KhadiIvory)
                    }

                    Button(
                        onClick = { confirmWipeDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerCrimson.copy(alpha = 0.2f),
                            contentColor = DangerCrimson
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("wipe_all_panic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Wipe All Local Data",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wipe Vault", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Transcripts",
                    tint = KhadiMuted
                )
            },
            placeholder = {
                Text(
                    text = "Search Hindi/English transcripts or Termux actions...",
                    color = KhadiMuted
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SwarajSaffron,
                unfocusedBorderColor = ObsidianCardBorder,
                focusedContainerColor = ObsidianSurface,
                unfocusedContainerColor = ObsidianSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transcript_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = languageFilter == LanguageFilter.ALL,
                onClick = { onLanguageFilterChange(LanguageFilter.ALL) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SwarajSaffron.copy(alpha = 0.22f),
                    selectedLabelColor = SwarajSaffron
                ),
                modifier = Modifier.testTag("filter_all_chip")
            )
            FilterChip(
                selected = languageFilter == LanguageFilter.HINDI_ONLY,
                onClick = { onLanguageFilterChange(LanguageFilter.HINDI_ONLY) },
                label = { Text("हिंदी (HI)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SwarajSaffron.copy(alpha = 0.22f),
                    selectedLabelColor = SwarajSaffron
                ),
                modifier = Modifier.testTag("filter_hi_chip")
            )
            FilterChip(
                selected = languageFilter == LanguageFilter.ENGLISH_ONLY,
                onClick = { onLanguageFilterChange(LanguageFilter.ENGLISH_ONLY) },
                label = { Text("ENG") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ChakraCyan.copy(alpha = 0.22f),
                    selectedLabelColor = ChakraCyan
                ),
                modifier = Modifier.testTag("filter_en_chip")
            )
            FilterChip(
                selected = languageFilter == LanguageFilter.ACTIONS_ONLY,
                onClick = { onLanguageFilterChange(LanguageFilter.ACTIONS_ONLY) },
                label = { Text("System/Termux") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AshokaEmerald.copy(alpha = 0.22f),
                    selectedLabelColor = AshokaEmerald
                ),
                modifier = Modifier.testTag("filter_actions_chip")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (transcripts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = KhadiMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No local transcripts match your filter.",
                        style = MaterialTheme.typography.titleMedium,
                        color = KhadiIvory
                    )
                    Text(
                        text = "Say \"Hey Gandhi\" on the Assistant tab to record offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KhadiMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("transcript_list"),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transcripts, key = { it.id }) { item ->
                    TranscriptItemCard(
                        item = item,
                        onTogglePin = { onTogglePin(item) },
                        onDelete = { onDeleteTranscript(item.id) },
                        onSpeak = { onSpeakResponse(item.assistantResponse) }
                    )
                }
            }
        }
    }

    if (confirmWipeDialog) {
        AlertDialog(
            onDismissRequest = { confirmWipeDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = "Wipe All Local Transcripts?",
                    color = KhadiIvory,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This permanently erases all recorded Hindi & English transcripts from the local Room SQLite database. Because Hey Gandhi has zero cloud sync, this cannot be undone.",
                    color = KhadiMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onWipeAllData()
                        confirmWipeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson),
                    modifier = Modifier.testTag("confirm_wipe_button")
                ) {
                    Text("Wipe Everything", color = KhadiIvory)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipeDialog = false }) {
                    Text("Cancel", color = KhadiMuted)
                }
            }
        )
    }

    if (exportedPreview != null) {
        val (title, content) = exportedPreview
        AlertDialog(
            onDismissRequest = onDismissExport,
            containerColor = ObsidianSurface,
            title = {
                Text(text = title, color = KhadiIvory, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .verticalScroll(rememberScrollState())
                        .background(ObsidianBg, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.labelSmall,
                        color = AshokaEmerald
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("HeyGandhiExport", content))
                            onDismissExport()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, title)
                                putExtra(Intent.EXTRA_TEXT, content)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Local Transcripts"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SwarajSaffron, contentColor = ObsidianBg)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExport) {
                    Text("Close", color = KhadiMuted)
                }
            }
        )
    }
}

@Composable
private fun TranscriptItemCard(
    item: TranscriptEntity,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onSpeak: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isHindi = item.detectedLanguage == "HI"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (item.isPinned) SwarajSaffron.copy(alpha = 0.65f) else ObsidianCardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("transcript_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        color = if (isHindi) SwarajSaffron.copy(alpha = 0.2f) else ChakraCyan.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isHindi) "हिंदी" else "ENG",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isHindi) SwarajSaffron else ChakraCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = sdf.format(Date(item.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = KhadiMuted
                    )

                    Text(
                        text = "• ${item.latencyMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = AshokaEmerald
                    )
                }

                Row {
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak transcript response",
                            tint = SwarajSaffron,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin transcript",
                            tint = if (item.isPinned) SwarajSaffron else KhadiMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("delete_transcript_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete transcript",
                            tint = DangerCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "“${item.userQuery}”",
                style = MaterialTheme.typography.titleMedium,
                color = KhadiIvory
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.assistantResponse,
                style = MaterialTheme.typography.bodyMedium,
                color = KhadiMuted
            )

            if (!item.executedAction.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AshokaEmerald.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Executed Action",
                            tint = AshokaEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.executedAction,
                            style = MaterialTheme.typography.labelSmall,
                            color = AshokaEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
